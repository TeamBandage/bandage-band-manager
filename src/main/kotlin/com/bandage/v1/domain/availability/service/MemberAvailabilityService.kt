package com.bandage.v1.domain.availability.service

import com.bandage.v1.domain.availability.dto.req.MemberAvailabilityRequest
import com.bandage.v1.domain.availability.dto.res.MemberAvailabilityResponse
import com.bandage.v1.domain.availability.model.AvailabilityException
import com.bandage.v1.domain.availability.model.MemberAvailability
import com.bandage.v1.domain.availability.model.WeeklyRule
import com.bandage.v1.domain.availability.repository.MemberAvailabilityRepository
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberAvailabilityService(
    private val memberAvailabilityRepository: MemberAvailabilityRepository,
) {
    /** 내 가용성 조회. 미등록 시 빈 응답 반환. */
    fun getMyAvailability(memberId: Long): MemberAvailabilityResponse =
        memberAvailabilityRepository
            .findByMemberId(memberId)
            ?.let { MemberAvailabilityResponse.from(it) }
            ?: MemberAvailabilityResponse.empty(memberId)

    /** 내 가용성 등록/수정(upsert). weeklyRules/exceptions 는 전체 교체. */
    @Transactional
    fun updateMyAvailability(
        memberId: Long,
        request: MemberAvailabilityRequest,
    ): MemberAvailabilityResponse {
        val rules = parseWeeklyRules(request)
        val exceptions = parseExceptions(request)

        val availability =
            memberAvailabilityRepository.findByMemberId(memberId)
                ?: MemberAvailability.create(memberId = memberId)

        availability.updateWeeklyRules(rules)
        availability.updateExceptions(exceptions)
        availability.updateNote(request.note)

        val saved = memberAvailabilityRepository.save(availability)
        return MemberAvailabilityResponse.from(saved)
    }

    // 슬롯/기간 검증은 Embeddable init 에서 수행되므로, 생성 실패 시 BusinessException 으로 변환한다.
    private fun parseWeeklyRules(request: MemberAvailabilityRequest): List<WeeklyRule> =
        try {
            request.toWeeklyRules()
        } catch (e: IllegalArgumentException) {
            throw BusinessException(ErrorCode.AVAILABILITY_INVALID)
        }

    private fun parseExceptions(request: MemberAvailabilityRequest): List<AvailabilityException> =
        try {
            request.toExceptions()
        } catch (e: IllegalArgumentException) {
            throw BusinessException(ErrorCode.AVAILABILITY_INVALID)
        }
}
