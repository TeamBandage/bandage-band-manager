package com.bandage.bandmanager.domain.schedule_bak.placement

import java.time.LocalDate

/**
 * 전략(HardConstraint/SoftScorer)이 평가하는 단일 후보 배치.
 * feasibility 에는 해당 슬롯의 멤버 가용성 평가 결과가 담긴다.
 */
data class PlacementCandidate(
    val date: LocalDate,
    val startSlot: Int,
    val durationSlots: Int,
    val feasibility: SlotFeasibility,
) {
    val endSlot: Int get() = startSlot + durationSlots
}

/**
 * SoftScorer 가 점수 계산에 참조하는 환경(보드 window, 현재까지 배치된 pending 블록).
 */
data class ScoringEnv(
    val windowFrom: LocalDate,
    val windowTo: LocalDate,
    val pendingBlocks: List<PendingBlock>,
)
