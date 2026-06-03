package com.bandage.v1.facade

import com.bandage.v1.domain.performance.repository.PerformanceRepository
import com.bandage.v1.domain.schedule.dto.req.AutoPlaceRequest
import com.bandage.v1.domain.schedule.dto.res.ProposalResponse
import com.bandage.v1.domain.schedule.model.PlacementOrigin
import com.bandage.v1.domain.schedule.model.ScheduleBlock
import com.bandage.v1.domain.schedule.model.ScheduleBlockTrack
import com.bandage.v1.domain.schedule.model.ScheduleBoard
import com.bandage.v1.domain.schedule.placement.AutoPlacer
import com.bandage.v1.domain.schedule.placement.AvailabilityCalculator
import com.bandage.v1.domain.schedule.placement.PendingBlock
import com.bandage.v1.domain.schedule.placement.PlaceableItem
import com.bandage.v1.domain.schedule.placement.PlacementContext
import com.bandage.v1.domain.schedule.placement.Proposal
import com.bandage.v1.domain.schedule.placement.strategy.BlockShape
import com.bandage.v1.domain.schedule.placement.strategy.CoverageGoal
import com.bandage.v1.domain.schedule.placement.strategy.StrategyComposition
import com.bandage.v1.domain.schedule.repository.ScheduleBlockRepository
import com.bandage.v1.domain.schedule.repository.ScheduleBlockTrackRepository
import com.bandage.v1.domain.schedule.repository.ScheduleBoardRepository
import com.bandage.v1.domain.schedule.service.ScheduleAuthService
import com.bandage.v1.domain.setlist.repository.SetlistTrackParticipantRepository
import com.bandage.v1.domain.setlist.repository.SetlistTrackRepository
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 합주 일정 자동 배치 Facade (PRD-2 Task 13).
 *
 * - preview: 저장 없이 제안만 반환
 * - autoPlace: 비고정 블록을 제거하고 제안을 실제 블록으로 저장(placementOrigin=AUTO)
 * - replan: 고정(pinned/anchored) 블록은 유지하고 나머지를 재배치
 * - anchorBlock: 블록을 고정(pinned + ANCHORED)
 */
@Service
class ScheduleAutoPlaceFacade(
    private val scheduleBoardRepository: ScheduleBoardRepository,
    private val scheduleBlockRepository: ScheduleBlockRepository,
    private val scheduleBlockTrackRepository: ScheduleBlockTrackRepository,
    private val performanceRepository: PerformanceRepository,
    private val setlistTrackRepository: SetlistTrackRepository,
    private val setlistTrackParticipantRepository: SetlistTrackParticipantRepository,
    private val availabilityCalculator: AvailabilityCalculator,
    private val autoPlacer: AutoPlacer,
    private val scheduleAuthService: ScheduleAuthService,
) {
    @Transactional(readOnly = true)
    fun preview(
        performanceId: UUID,
        boardId: UUID,
        memberId: Long,
        request: AutoPlaceRequest,
    ): ProposalResponse {
        scheduleAuthService.validatePerformanceParticipant(performanceId, memberId)
        val board = getBoardOrThrow(performanceId, boardId)
        val context = buildContext(performanceId, board, request, anchored = emptyList(), perTrackOverrides = emptyMap())
        return ProposalResponse.from(autoPlacer.propose(context))
    }

    @Transactional
    fun autoPlace(
        performanceId: UUID,
        boardId: UUID,
        memberId: Long,
        request: AutoPlaceRequest,
    ): ProposalResponse {
        scheduleAuthService.validatePerformanceManager(performanceId, memberId)
        val board = getBoardOrThrow(performanceId, boardId)
        ensureEditable(board)

        deleteNonPinnedBlocks(boardId)
        val context = buildContext(performanceId, board, request, anchored = emptyList(), perTrackOverrides = emptyMap())
        val proposal = autoPlacer.propose(context)
        val blockIdByIndex = persistProposal(board, proposal)
        return ProposalResponse.from(proposal, blockIdByIndex)
    }

    @Transactional
    fun replan(
        performanceId: UUID,
        boardId: UUID,
        memberId: Long,
        request: AutoPlaceRequest,
    ): ProposalResponse {
        scheduleAuthService.validatePerformanceManager(performanceId, memberId)
        val board = getBoardOrThrow(performanceId, boardId)
        ensureEditable(board)

        // 고정 블록 → anchored pending + 트랙별 잔여 커버리지 계산
        val pinnedBlocks = scheduleBlockRepository.findAllByBoardId(boardId).filter { it.pinned }
        val tracksByBlock = trackIdsByBlock(pinnedBlocks.map { it.id })
        val participantsByTrack = participantsByTrack(tracksByBlock.values.flatten().distinct())

        val anchored =
            pinnedBlocks.map { block ->
                val trackIds = tracksByBlock[block.id].orEmpty()
                val members = trackIds.flatMap { participantsByTrack[it].orEmpty() }.toSet()
                PendingBlock(block.date, block.startSlot, block.durationSlots, members)
            }
        val pinnedCountByTrack =
            pinnedBlocks
                .flatMap { tracksByBlock[it.id].orEmpty() }
                .groupingBy { it }
                .eachCount()

        deleteNonPinnedBlocks(boardId)
        val context = buildContext(performanceId, board, request, anchored = anchored, perTrackOverrides = pinnedCountByTrack)
        val proposal = autoPlacer.propose(context)
        val blockIdByIndex = persistProposal(board, proposal)
        return ProposalResponse.from(proposal, blockIdByIndex)
    }

    @Transactional
    fun anchorBlock(
        performanceId: UUID,
        boardId: UUID,
        blockId: UUID,
        memberId: Long,
    ) {
        scheduleAuthService.validatePerformanceManager(performanceId, memberId)
        val board = getBoardOrThrow(performanceId, boardId)
        ensureEditable(board)
        val block =
            scheduleBlockRepository.findByIdOrNull(blockId)
                ?: throw BusinessException(ErrorCode.SCHEDULE_BLOCK_NOT_FOUND)
        if (block.board.id != boardId) throw BusinessException(ErrorCode.SCHEDULE_BLOCK_NOT_FOUND)
        block.pin()
        block.updatePlacementOrigin(PlacementOrigin.ANCHORED)
    }

    // ===== 내부 =====

    private fun buildContext(
        performanceId: UUID,
        board: ScheduleBoard,
        request: AutoPlaceRequest,
        anchored: List<PendingBlock>,
        perTrackOverrides: Map<UUID, Int>,
    ): PlacementContext {
        val windowFrom = request.windowFrom ?: board.windowFrom ?: throw BusinessException(ErrorCode.SCHEDULE_WINDOW_REQUIRED)
        val windowTo = request.windowTo ?: board.windowTo ?: throw BusinessException(ErrorCode.SCHEDULE_WINDOW_REQUIRED)

        val strategy = applyOverrides(request)
        val sessionsPerTrack = strategy.coverageGoal.sessionsPerTrack

        val candidateTracks = candidateTrackIds(performanceId)
        if (candidateTracks.isEmpty()) throw BusinessException(ErrorCode.SCHEDULE_NO_PLACEABLE_TRACK)
        val participantsByTrack = participantsByTrack(candidateTracks)

        val items =
            candidateTracks.mapNotNull { trackId ->
                val remaining = sessionsPerTrack - (perTrackOverrides[trackId] ?: 0)
                if (remaining <= 0) {
                    null
                } else {
                    PlaceableItem(trackId, participantsByTrack[trackId].orEmpty().toSet(), remaining)
                }
            }

        val members = items.flatMap { it.memberIds }.distinct()
        val availabilityContext = availabilityCalculator.loadContext(members)

        return PlacementContext(
            windowFrom = windowFrom,
            windowTo = windowTo,
            items = items,
            strategy = strategy,
            availabilityContext = availabilityContext,
            anchored = anchored,
        )
    }

    private fun applyOverrides(request: AutoPlaceRequest): StrategyComposition {
        val base = request.preset.composition()
        val blockShape = request.durationSlots?.let { BlockShape(it) } ?: base.blockShape
        val coverage = request.sessionsPerTrack?.let { CoverageGoal(it) } ?: base.coverageGoal
        return base.copy(blockShape = blockShape, coverageGoal = coverage)
    }

    private fun persistProposal(
        board: ScheduleBoard,
        proposal: Proposal,
    ): Map<Int, UUID> {
        val idByIndex = mutableMapOf<Int, UUID>()
        proposal.blocks.forEachIndexed { index, pb ->
            val block =
                scheduleBlockRepository.save(
                    ScheduleBlock.create(
                        board = board,
                        date = pb.date,
                        startSlot = pb.startSlot,
                        durationSlots = pb.durationSlots,
                        placementOrigin = PlacementOrigin.AUTO,
                    ),
                )
            scheduleBlockTrackRepository.save(
                ScheduleBlockTrack.create(block = block, setlistTrackId = pb.trackId, ordinal = 0),
            )
            idByIndex[index] = block.id
        }
        return idByIndex
    }

    private fun deleteNonPinnedBlocks(boardId: UUID) {
        val nonPinned = scheduleBlockRepository.findAllByBoardId(boardId).filter { !it.pinned }
        nonPinned.forEach { block ->
            scheduleBlockTrackRepository.deleteAllByBlockId(block.id)
            scheduleBlockRepository.delete(block)
        }
    }

    private fun candidateTrackIds(performanceId: UUID): List<UUID> {
        val performance =
            performanceRepository.findByIdOrNull(performanceId)
                ?: throw BusinessException(ErrorCode.PERFORMANCE_NOT_FOUND)
        val setlistIds = performance.setlists.map { it.setlistId }
        if (setlistIds.isEmpty()) return emptyList()
        return setlistTrackRepository.findAllBySetlistIdIn(setlistIds).map { it.id }
    }

    private fun participantsByTrack(trackIds: List<UUID>): Map<UUID, List<Long>> {
        if (trackIds.isEmpty()) return emptyMap()
        val tracks = setlistTrackRepository.findAllById(trackIds)
        return setlistTrackParticipantRepository
            .findAllByTrackIn(tracks)
            .groupBy({ it.track.id }, { it.memberId })
    }

    private fun trackIdsByBlock(blockIds: List<UUID>): Map<UUID, List<UUID>> {
        if (blockIds.isEmpty()) return emptyMap()
        return scheduleBlockTrackRepository
            .findAllByBlockIdIn(blockIds)
            .sortedBy { it.ordinal }
            .groupBy({ it.block.id }, { it.setlistTrackId })
    }

    private fun ensureEditable(board: ScheduleBoard) {
        if (board.confirmed) throw BusinessException(ErrorCode.SCHEDULE_BOARD_ALREADY_CONFIRMED)
    }

    private fun getBoardOrThrow(
        performanceId: UUID,
        boardId: UUID,
    ): ScheduleBoard {
        val board =
            scheduleBoardRepository.findByIdOrNull(boardId)
                ?: throw BusinessException(ErrorCode.SCHEDULE_BOARD_NOT_FOUND)
        if (board.performanceId != performanceId) throw BusinessException(ErrorCode.SCHEDULE_BOARD_NOT_FOUND)
        return board
    }
}
