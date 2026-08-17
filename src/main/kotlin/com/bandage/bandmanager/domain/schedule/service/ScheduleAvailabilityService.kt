package com.bandage.bandmanager.domain.schedule.service

import com.bandage.bandmanager.domain.availability.repository.MemberAvailabilityRepository
import com.bandage.bandmanager.domain.schedule.dto.res.SlotAvailabilityResponse
import com.bandage.bandmanager.domain.schedule.model.Slot
import com.bandage.bandmanager.domain.setlist.repository.SetlistTrackParticipantRepository
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * 셋리스트 참여 멤버들의 슬롯별 가용 현황 조회.
 *
 * 시간표 화면이 기간 단위로 벌크 조회해 캐싱하는 용도이며,
 * 자동배치도 같은 인덱스(MemberAvailabilityIndex)를 써서 후보 구간을 거른다.
 */
@Service
@Transactional(readOnly = true)
class ScheduleAvailabilityService(
    private val setlistTrackParticipantRepository: SetlistTrackParticipantRepository,
    private val memberAvailabilityRepository: MemberAvailabilityRepository,
    private val scheduleAuthService: ScheduleAuthService,
) {
    /**
     * [from, to] 구간의 모든 (날짜, 슬롯) 에 대해 셋리스트 참여 멤버의 가용 여부를 펼쳐 반환한다.
     *
     * 하루 48칸을 모두 내려주므로 클라이언트는 슬롯 호버링 시 추가 조회 없이 표시할 수 있다.
     * 트랙별 판정은 트랙 참여자 목록과 availableMemberIds 를 교집합해 클라이언트에서 계산한다.
     */
    fun getSlotAvailabilities(
        setlistId: UUID,
        memberId: Long,
        from: LocalDate,
        to: LocalDate,
    ): List<SlotAvailabilityResponse> {
        scheduleAuthService.validateSetlistParticipant(setlistId, memberId)
        validateRange(from, to)

        val participantIds =
            setlistTrackParticipantRepository
                .findAllBySetlistId(setlistId)
                .map { it.memberId }
                .distinct()
        if (participantIds.isEmpty()) return emptyList()

        val index = buildIndex(participantIds, from, to)

        val result = ArrayList<SlotAvailabilityResponse>(dayCountOf(from, to) * Slot.SLOTS_PER_DAY)
        var date = from
        while (!date.isAfter(to)) {
            for (slot in 0 until Slot.SLOTS_PER_DAY) {
                val available = index.availableMembersAt(date, slot)
                result +=
                    SlotAvailabilityResponse(
                        date = date,
                        slot = slot,
                        availableMemberIds = available,
                        unavailableMemberIds = participantIds - available.toSet(),
                    )
            }
            date = date.plusDays(1)
        }
        return result
    }

    /** 자동배치와 공유하는 인덱스 생성. 가용성 미등록 멤버는 항상 가용으로 처리된다. */
    fun buildIndex(
        memberIds: Collection<Long>,
        from: LocalDate,
        to: LocalDate,
    ): MemberAvailabilityIndex =
        MemberAvailabilityIndex.build(
            memberIds = memberIds,
            availabilities = memberAvailabilityRepository.findAllByMemberIdIn(memberIds),
            from = from,
            to = to,
        )

    private fun validateRange(
        from: LocalDate,
        to: LocalDate,
    ) {
        if (from.isAfter(to) || ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw BusinessException(ErrorCode.AVAILABILITY_RANGE_INVALID)
        }
    }

    private fun dayCountOf(
        from: LocalDate,
        to: LocalDate,
    ): Int = (ChronoUnit.DAYS.between(from, to) + 1).toInt()

    companion object {
        /** 하루 48칸을 모두 펼치므로 가용성 조회(366일)보다 좁게 잡는다. 31일 × 48 = 1488 건. */
        const val MAX_RANGE_DAYS: Long = 31
    }
}
