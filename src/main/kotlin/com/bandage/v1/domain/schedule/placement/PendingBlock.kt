package com.bandage.v1.domain.schedule.placement

import java.time.LocalDate

/**
 * 같은 보드에서 이미 배치된(확정 전) 블록. AutoPlacer 가 해(解)를 구성하는 동안의 임시 점유를 표현한다.
 * 같은 멤버가 시간상 겹치는 다른 pending 블록에 동시에 들어갈 수 없도록 충돌 검사에 사용한다.
 */
data class PendingBlock(
    val date: LocalDate,
    val startSlot: Int,
    val durationSlots: Int,
    val memberIds: Set<Long>,
) {
    val endSlot: Int get() = startSlot + durationSlots

    fun overlaps(
        otherDate: LocalDate,
        otherStart: Int,
        otherDuration: Int,
    ): Boolean {
        if (otherDate != date) return false
        val otherEnd = otherStart + otherDuration
        return startSlot < otherEnd && endSlot > otherStart
    }
}
