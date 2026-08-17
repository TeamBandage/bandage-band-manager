package com.bandage.bandmanager.domain.schedule.service

import com.bandage.bandmanager.domain.availability.model.MemberAvailability
import com.bandage.bandmanager.domain.schedule.model.Slot
import java.time.LocalDate

/**
 * 조회 기간 안의 (날짜, 슬롯) 마다 어떤 멤버가 가용한지 미리 펼쳐 둔 인덱스.
 *
 * MemberAvailability.isAvailableAt 은 호출마다 예외/규칙 리스트를 선형 탐색하고 새 리스트를 할당한다.
 * 자동배치는 (윈도우 일수 × 슬롯 × 멤버 × 조각) 만큼 가용 판정을 반복하므로 그대로 쓰면 호출이 수만 번이 된다.
 * 여기서 한 번만 전개해 두고 이후에는 배열 조회만 한다.
 *
 * 가용성 미등록 멤버는 제약 없음(항상 가용)으로 본다.
 */
class MemberAvailabilityIndex private constructor(
    private val memberIds: List<Long>,
    private val from: LocalDate,
    private val dayCount: Int,
    // [멤버 순서][날짜 오프셋 * SLOTS_PER_DAY + 슬롯] = 가용 여부
    private val flags: Array<BooleanArray>,
) {
    private val indexByMemberId: Map<Long, Int> = memberIds.withIndex().associate { (i, id) -> id to i }

    /** 인덱스가 아는 전체 멤버. 조회 대상이 아닌 멤버를 넘기면 판정에서 제외된다. */
    val knownMemberIds: List<Long> get() = memberIds

    /** [date, slot] 한 칸에 가용한 멤버 (memberIds 중에서). */
    fun availableMembersAt(
        date: LocalDate,
        slot: Int,
    ): List<Long> {
        val offset = offsetOf(date, slot) ?: return emptyList()
        return memberIds.filterIndexed { i, _ -> flags[i][offset] }
    }

    /** 특정 멤버가 [date, slot] 한 칸에 가용한지. 인덱스 범위 밖이면 false. */
    fun isAvailable(
        memberId: Long,
        date: LocalDate,
        slot: Int,
    ): Boolean {
        val memberIdx = indexByMemberId[memberId] ?: return false
        val offset = offsetOf(date, slot) ?: return false
        return flags[memberIdx][offset]
    }

    /**
     * 주어진 멤버 전원이 구간 [startSlot, endSlot) 전체에 가용한지.
     * 자동배치가 후보 구간을 거를 때 쓰는 판정이다.
     */
    fun allAvailableThrough(
        members: Collection<Long>,
        date: LocalDate,
        startSlot: Int,
        endSlot: Int,
    ): Boolean {
        if (members.isEmpty()) return true
        val indices = members.map { indexByMemberId[it] ?: return false }
        for (slot in startSlot until endSlot) {
            val offset = offsetOf(date, slot) ?: return false
            if (indices.any { !flags[it][offset] }) return false
        }
        return true
    }

    private fun offsetOf(
        date: LocalDate,
        slot: Int,
    ): Int? {
        if (slot !in 0 until Slot.SLOTS_PER_DAY) return null
        val dayOffset = (date.toEpochDay() - from.toEpochDay()).toInt()
        if (dayOffset !in 0 until dayCount) return null
        return dayOffset * Slot.SLOTS_PER_DAY + slot
    }

    companion object {
        /**
         * [from, to] (양끝 포함) 구간에 대해 인덱스를 만든다.
         *
         * availabilities 에 없는 멤버는 가용성 미등록으로 보아 전 구간 가용 처리한다.
         */
        fun build(
            memberIds: Collection<Long>,
            availabilities: Collection<MemberAvailability>,
            from: LocalDate,
            to: LocalDate,
        ): MemberAvailabilityIndex {
            require(!from.isAfter(to)) { "from($from) must be on or before to($to)" }
            val ids = memberIds.distinct()
            val dayCount = (to.toEpochDay() - from.toEpochDay()).toInt() + 1
            val byMemberId = availabilities.associateBy { it.memberId }

            val flags =
                Array(ids.size) { memberIdx ->
                    val availability =
                        byMemberId[ids[memberIdx]]
                            // 미등록 멤버는 전 구간 가용
                            ?: return@Array BooleanArray(dayCount * Slot.SLOTS_PER_DAY) { true }
                    BooleanArray(dayCount * Slot.SLOTS_PER_DAY).also { arr ->
                        for (dayOffset in 0 until dayCount) {
                            val date = from.plusDays(dayOffset.toLong())
                            for (slot in 0 until Slot.SLOTS_PER_DAY) {
                                arr[dayOffset * Slot.SLOTS_PER_DAY + slot] =
                                    availability.isAvailableAt(date, slot, slot + 1)
                            }
                        }
                    }
                }
            return MemberAvailabilityIndex(ids, from, dayCount, flags)
        }
    }
}
