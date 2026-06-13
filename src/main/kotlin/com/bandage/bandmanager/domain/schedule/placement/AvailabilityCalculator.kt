package com.bandage.bandmanager.domain.schedule.placement

import com.bandage.bandmanager.domain.availability.model.AvailabilityException
import com.bandage.bandmanager.domain.availability.model.AvailabilityKind
import com.bandage.bandmanager.domain.availability.model.MemberAvailability
import com.bandage.bandmanager.domain.availability.repository.MemberAvailabilityRepository
import com.bandage.bandmanager.domain.jam.model.JamReservation
import com.bandage.bandmanager.domain.jam.repository.JamReservationRepository
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * 가용성/충돌 계산기.
 *
 * 같은 멤버 집합에 대해 수많은 후보 슬롯을 평가하는 AutoPlacer 를 위해
 * 데이터(가용성/예약)는 [loadContext] 로 한 번만 선로딩하고, [evaluate] 는 순수 in-memory 연산으로 동작한다.
 *
 * 평가 규칙(멤버별):
 * 1. MemberAvailability 가 있으면 그 가용 시간 내여야 함(BLOCKED 예외/주간규칙 적용). 없으면 가용으로 간주(낙관적).
 * 2. 이미 확정된 JamReservation 과 시간이 겹치면 충돌.
 * 3. 같은 보드의 pending 블록과 겹치면 충돌.
 *
 * 시간은 30분 48슬롯 체계. 보드 근무시간 등 보드 레벨 제약은 전략(HardConstraint)에서 별도 처리한다.
 */
@Component
class AvailabilityCalculator(
    private val memberAvailabilityRepository: MemberAvailabilityRepository,
    private val jamReservationRepository: JamReservationRepository,
) {
    fun loadContext(memberIds: List<Long>): AvailabilityContext {
        if (memberIds.isEmpty()) return AvailabilityContext(emptyMap(), emptyMap())
        val distinct = memberIds.distinct()
        val availability = memberAvailabilityRepository.findAllByMemberIdIn(distinct).associateBy { it.memberId }
        val reservations = jamReservationRepository.findAllByMemberIdIn(distinct).groupBy { it.memberId }
        return AvailabilityContext(availability, reservations)
    }

    fun evaluate(
        context: AvailabilityContext,
        date: LocalDate,
        startSlot: Int,
        durationSlots: Int,
        memberIds: List<Long>,
        pendingBlocks: List<PendingBlock> = emptyList(),
    ): SlotFeasibility {
        val reqEnd = startSlot + durationSlots
        val available = mutableListOf<Long>()
        val conflicts = mutableListOf<Conflict>()

        memberIds.distinct().forEach { memberId ->
            val reason = firstConflictReason(context, memberId, date, startSlot, reqEnd, durationSlots, pendingBlocks)
            if (reason == null) {
                available.add(memberId)
            } else {
                conflicts.add(Conflict(memberId = memberId, reason = reason))
            }
        }

        return SlotFeasibility(
            date = date,
            startSlot = startSlot,
            durationSlots = durationSlots,
            totalMembers = memberIds.distinct().size,
            availableMembers = available,
            conflicts = conflicts,
        )
    }

    private fun firstConflictReason(
        context: AvailabilityContext,
        memberId: Long,
        date: LocalDate,
        startSlot: Int,
        endSlot: Int,
        durationSlots: Int,
        pendingBlocks: List<PendingBlock>,
    ): ConflictReason? {
        // 1. 글로벌 가용성
        val availability = context.availabilityByMember[memberId]
        if (availability != null && !isAvailable(availability, date, startSlot, endSlot)) {
            return ConflictReason.UNAVAILABLE
        }
        // 2. 확정 Jam 예약 충돌
        val reservations = context.reservationsByMember[memberId].orEmpty()
        if (reservations.any { overlapsReservation(it, date, startSlot, endSlot) }) {
            return ConflictReason.JAM_RESERVATION
        }
        // 3. 같은 보드 pending 충돌
        if (pendingBlocks.any { it.memberIds.contains(memberId) && it.overlaps(date, startSlot, durationSlots) }) {
            return ConflictReason.PENDING_BLOCK
        }
        return null
    }

    private fun isAvailable(
        availability: MemberAvailability,
        date: LocalDate,
        reqStart: Int,
        reqEnd: Int,
    ): Boolean {
        val dayExceptions = availability.exceptions.filter { it.date == date }
        // BLOCKED 예외가 요청 구간과 겹치면 불가
        if (dayExceptions.any { it.kind == AvailabilityKind.BLOCKED && exceptionOverlaps(it, reqStart, reqEnd) }) {
            return false
        }
        // AVAILABLE 예외가 요청 구간을 포함하면 가용(주간규칙 무관)
        if (dayExceptions.any { it.kind == AvailabilityKind.AVAILABLE && exceptionCovers(it, reqStart, reqEnd) }) {
            return true
        }
        // 주간 반복 규칙으로 요청 구간이 완전히 덮이면 가용
        return availability.weeklyRules.any {
            it.isEffectiveOn(date) && it.startSlot <= reqStart && reqEnd <= it.endSlot
        }
    }

    private fun exceptionOverlaps(
        exception: AvailabilityException,
        reqStart: Int,
        reqEnd: Int,
    ): Boolean {
        if (exception.isAllDay) return true
        val s = exception.startSlot ?: return true
        val e = exception.endSlot ?: return true
        return s < reqEnd && e > reqStart
    }

    private fun exceptionCovers(
        exception: AvailabilityException,
        reqStart: Int,
        reqEnd: Int,
    ): Boolean {
        if (exception.isAllDay) return true
        val s = exception.startSlot ?: return true
        val e = exception.endSlot ?: return true
        return s <= reqStart && reqEnd <= e
    }

    private fun overlapsReservation(
        reservation: JamReservation,
        date: LocalDate,
        startSlot: Int,
        endSlot: Int,
    ): Boolean {
        val reqStart = date.atStartOfDay().plusMinutes(startSlot.toLong() * MINUTES_PER_SLOT)
        val reqEnd = date.atStartOfDay().plusMinutes(endSlot.toLong() * MINUTES_PER_SLOT)
        return reservation.startAt.isBefore(reqEnd) && reservation.endAt.isAfter(reqStart)
    }

    companion object {
        const val MINUTES_PER_SLOT: Long = 30
    }
}

/**
 * 선로딩된 가용성/예약 데이터. [AvailabilityCalculator.evaluate] 가 in-memory 로 참조한다.
 */
class AvailabilityContext(
    val availabilityByMember: Map<Long, MemberAvailability>,
    val reservationsByMember: Map<Long, List<JamReservation>>,
)
