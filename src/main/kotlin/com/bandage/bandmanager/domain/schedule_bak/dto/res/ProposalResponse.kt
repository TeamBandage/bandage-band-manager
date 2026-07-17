package com.bandage.bandmanager.domain.schedule_bak.dto.res

import com.bandage.bandmanager.domain.schedule_bak.placement.Proposal
import java.time.LocalDate
import java.util.UUID

/**
 * 자동 배치 제안 결과 응답. preview 시 blockId 는 null, 실제 배치(auto-place/replan) 시 생성된 블록 id 가 채워진다.
 */
data class ProposalResponse(
    val placedCount: Int,
    val totalScore: Double,
    val blocks: List<BlockDto>,
    val unplaced: List<UnplacedDto>,
) {
    data class BlockDto(
        val blockId: UUID?,
        val trackId: UUID,
        val date: LocalDate,
        val startSlot: Int,
        val durationSlots: Int,
        val memberIds: Set<Long>,
        val score: Double,
        val availabilityRatio: Double,
        val placementReasons: List<String>,
    )

    data class UnplacedDto(
        val trackId: UUID,
        val reason: String,
        val nearMisses: List<NearMissDto>,
    )

    data class NearMissDto(
        val date: LocalDate,
        val startSlot: Int,
        val durationSlots: Int,
        val availabilityRatio: Double,
        val reason: String,
    )

    companion object {
        fun from(
            proposal: Proposal,
            blockIdByIndex: Map<Int, UUID> = emptyMap(),
        ): ProposalResponse =
            ProposalResponse(
                placedCount = proposal.placedCount,
                totalScore = proposal.totalScore,
                blocks =
                    proposal.blocks.mapIndexed { index, b ->
                        BlockDto(
                            blockId = blockIdByIndex[index],
                            trackId = b.trackId,
                            date = b.date,
                            startSlot = b.startSlot,
                            durationSlots = b.durationSlots,
                            memberIds = b.memberIds,
                            score = b.score,
                            availabilityRatio = b.availabilityRatio,
                            placementReasons = b.placementReasons,
                        )
                    },
                unplaced =
                    proposal.unplaced.map { u ->
                        UnplacedDto(
                            trackId = u.trackId,
                            reason = u.reason,
                            nearMisses =
                                u.nearMisses.map { nm ->
                                    NearMissDto(nm.date, nm.startSlot, nm.durationSlots, nm.availabilityRatio, nm.reason)
                                },
                        )
                    },
            )
    }
}
