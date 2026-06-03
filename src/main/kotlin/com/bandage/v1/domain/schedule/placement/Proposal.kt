package com.bandage.v1.domain.schedule.placement

import java.time.LocalDate
import java.util.UUID

/**
 * 자동 배치 제안의 단일 블록.
 */
data class ProposedBlock(
    val trackId: UUID,
    val date: LocalDate,
    val startSlot: Int,
    val durationSlots: Int,
    val memberIds: Set<Long>,
    val score: Double,
    val availabilityRatio: Double,
    val placementReasons: List<String>,
) {
    val endSlot: Int get() = startSlot + durationSlots

    fun toPendingBlock(): PendingBlock = PendingBlock(date, startSlot, durationSlots, memberIds)
}

/**
 * 배치하지 못한 항목과 그 이유(가장 유력했던 후보들의 충돌 사유).
 */
data class UnplacedItem(
    val trackId: UUID,
    val memberIds: Set<Long>,
    val reason: String,
    val nearMisses: List<NearMiss>,
)

/**
 * 배치되진 못했지만 유력했던 후보(설명용).
 */
data class NearMiss(
    val date: LocalDate,
    val startSlot: Int,
    val durationSlots: Int,
    val availabilityRatio: Double,
    val reason: String,
)

/**
 * 자동 배치 제안 결과.
 */
data class Proposal(
    val blocks: List<ProposedBlock>,
    val unplaced: List<UnplacedItem>,
    val totalScore: Double,
) {
    val placedCount: Int get() = blocks.size
}
