package com.bandage.bandmanager.domain.availability.service

import com.bandage.bandmanager.domain.availability.dto.req.MemberAvailabilityRequest
import com.bandage.bandmanager.domain.availability.dto.res.MemberAvailabilityResponse
import com.bandage.bandmanager.domain.availability.model.AvailabilityException
import com.bandage.bandmanager.domain.availability.model.MemberAvailability
import com.bandage.bandmanager.domain.availability.model.WeeklyRule
import com.bandage.bandmanager.domain.availability.model.WeeklyRule.Companion.SLOTS_PER_DAY
import com.bandage.bandmanager.domain.availability.repository.MemberAvailabilityRepository
import com.bandage.bandmanager.global.common.response.ScheduleSlotResponse
import com.bandage.bandmanager.global.common.response.ScheduleSlotType
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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

    /**
     * 조회 기간 [from, to] 에 속한 가용 슬롯을 날짜별로 전개해 반환한다.
     * 규칙/예외를 서버에서 구체 슬롯으로 풀어주므로 클라이언트는 추가 계산 없이 그대로 렌더할 수 있다.
     * 미등록 멤버는 빈 리스트. 최대 366일까지 조회 가능.
     */
    fun getMySlots(
        memberId: Long,
        from: LocalDate,
        to: LocalDate,
    ): List<ScheduleSlotResponse> {
        if (from.isAfter(to) || ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw BusinessException(ErrorCode.AVAILABILITY_RANGE_INVALID)
        }
        val availability = memberAvailabilityRepository.findByMemberId(memberId) ?: return emptyList()

        val slots = mutableListOf<ScheduleSlotResponse>()
        var date = from
        while (!date.isAfter(to)) {
            // 하루 48슬롯을 가용 판정에 통과시켜 연속 구간(run-length)으로 묶는다.
            var start = -1
            for (slot in 0 until SLOTS_PER_DAY) {
                val available = availability.isAvailableAt(date, slot, slot + 1)
                if (available && start < 0) start = slot
                if (!available && start >= 0) {
                    slots += slotOf(date, start, slot)
                    start = -1
                }
            }
            if (start >= 0) slots += slotOf(date, start, SLOTS_PER_DAY)
            date = date.plusDays(1)
        }
        return slots
    }

    private fun slotOf(
        date: LocalDate,
        startSlot: Int,
        endSlot: Int,
    ): ScheduleSlotResponse =
        ScheduleSlotResponse(
            date = date,
            startSlot = startSlot,
            endSlot = endSlot,
            type = ScheduleSlotType.AVAILABLE,
        )

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

    companion object {
        const val MAX_RANGE_DAYS: Long = 366
    }
}
