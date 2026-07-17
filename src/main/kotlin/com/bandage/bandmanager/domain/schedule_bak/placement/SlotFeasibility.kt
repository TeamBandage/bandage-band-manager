package com.bandage.bandmanager.domain.schedule_bak.placement

import java.time.LocalDate

/**
 * 한 멤버의 후보 슬롯 배치 충돌.
 */
data class Conflict(
    val memberId: Long,
    val reason: ConflictReason,
    val detail: String? = null,
)

/**
 * 특정 후보 슬롯(date, startSlot, durationSlots)에 대한 멤버 집합의 가용성 평가 결과.
 *
 * - feasible: 충돌이 하나도 없으면 true
 * - availableMembers: 충돌 없이 참여 가능한 멤버
 * - conflicts: 충돌 목록(멤버별 사유)
 * - availabilityRatio: 전체 대비 가용 멤버 비율(0.0~1.0). 소프트 점수 계산에 사용
 */
data class SlotFeasibility(
    val date: LocalDate,
    val startSlot: Int,
    val durationSlots: Int,
    val totalMembers: Int,
    val availableMembers: List<Long>,
    val conflicts: List<Conflict>,
) {
    val feasible: Boolean get() = conflicts.isEmpty()

    val availabilityRatio: Double
        get() = if (totalMembers == 0) 0.0 else availableMembers.size.toDouble() / totalMembers
}
