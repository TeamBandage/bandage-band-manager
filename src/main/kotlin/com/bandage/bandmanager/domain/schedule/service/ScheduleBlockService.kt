package com.bandage.bandmanager.domain.schedule.service

import com.bandage.bandmanager.domain.performance.repository.PerformanceRepository
import com.bandage.bandmanager.domain.schedule.dto.req.ScheduleBlockUpsertRequest
import com.bandage.bandmanager.domain.schedule.dto.res.ScheduleBlockResponse
import com.bandage.bandmanager.domain.schedule.model.RecurrenceRule
import com.bandage.bandmanager.domain.schedule.model.ScheduleBlock
import com.bandage.bandmanager.domain.schedule.model.ScheduleBlockTrack
import com.bandage.bandmanager.domain.schedule.model.ScheduleBoard
import com.bandage.bandmanager.domain.schedule.repository.ScheduleBlockRepository
import com.bandage.bandmanager.domain.schedule.repository.ScheduleBlockTrackRepository
import com.bandage.bandmanager.domain.schedule.repository.ScheduleBoardRepository
import com.bandage.bandmanager.domain.selection.model.PracticeWindow
import com.bandage.bandmanager.domain.setlist.repository.SetlistTrackRepository
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ScheduleBlockService(
    private val scheduleBoardRepository: ScheduleBoardRepository,
    private val scheduleBlockRepository: ScheduleBlockRepository,
    private val scheduleBlockTrackRepository: ScheduleBlockTrackRepository,
    private val performanceRepository: PerformanceRepository,
    private val setlistTrackRepository: SetlistTrackRepository,
    private val scheduleAuthService: ScheduleAuthService,
) {
    @Transactional
    fun upsertBlock(
        performanceId: UUID,
        boardId: UUID,
        blockId: UUID,
        memberId: Long,
        request: ScheduleBlockUpsertRequest,
    ): ScheduleBlockResponse {
        scheduleAuthService.validatePerformanceManager(performanceId, memberId)
        val board = getBoardOrThrow(performanceId, boardId)
        ensureBoardEditable(board)
        validateSlot(request.startSlot, request.durationSlots)
        board.practiceWindowOrNull()?.let { validateDateInWindow(request.date, it) }
        validateTracksInPerformance(performanceId, request.trackIds)

        val recurrence = toRecurrenceRule(request)
        val existing = scheduleBlockRepository.findByIdOrNull(blockId)
        val block =
            if (existing != null) {
                if (existing.board.id != boardId) {
                    throw BusinessException(ErrorCode.SCHEDULE_BLOCK_NOT_FOUND)
                }
                existing.reposition(request.date, request.startSlot, request.durationSlots)
                existing.updatePaletteIndex(request.paletteIndex)
                existing.updateTitleOverride(request.titleOverride)
                existing.updateNote(request.note)
                existing.updateRecurrenceRule(recurrence)
                request.pinned?.let { if (it) existing.pin() else existing.unpin() }
                existing
            } else {
                ScheduleBlock
                    .create(
                        id = blockId,
                        board = board,
                        date = request.date,
                        startSlot = request.startSlot,
                        durationSlots = request.durationSlots,
                        paletteIndex = request.paletteIndex,
                        titleOverride = request.titleOverride,
                        note = request.note,
                        recurrenceRule = recurrence,
                    ).apply {
                        request.pinned?.let { if (it) pin() }
                    }
            }
        val saved = scheduleBlockRepository.save(block)
        val trackIds = replaceBlockTracks(saved, request.trackIds)
        return ScheduleBlockResponse.from(saved, trackIds)
    }

    @Transactional
    fun deleteBlock(
        performanceId: UUID,
        boardId: UUID,
        blockId: UUID,
        memberId: Long,
    ) {
        scheduleAuthService.validatePerformanceManager(performanceId, memberId)
        val board = getBoardOrThrow(performanceId, boardId)
        ensureBoardEditable(board)
        val block = getBlockOrThrow(boardId, blockId)
        scheduleBlockTrackRepository.deleteAllByBlockId(block.id)
        scheduleBlockRepository.delete(block)
    }

    @Transactional
    fun setPin(
        performanceId: UUID,
        boardId: UUID,
        blockId: UUID,
        memberId: Long,
        pinned: Boolean,
    ): ScheduleBlockResponse {
        scheduleAuthService.validatePerformanceManager(performanceId, memberId)
        val board = getBoardOrThrow(performanceId, boardId)
        ensureBoardEditable(board)
        val block = getBlockOrThrow(boardId, blockId)
        if (pinned) block.pin() else block.unpin()
        val trackIds =
            scheduleBlockTrackRepository
                .findAllByBlockIdOrderByOrdinalAsc(block.id)
                .map { it.setlistTrackId }
        return ScheduleBlockResponse.from(block, trackIds)
    }

    /** 블록의 트랙 매핑을 전체 교체하고, 적용된 trackId 목록을 순서대로 반환한다. */
    private fun replaceBlockTracks(
        block: ScheduleBlock,
        trackIds: List<UUID>,
    ): List<UUID> {
        scheduleBlockTrackRepository.deleteAllByBlockId(block.id)
        val distinct = trackIds.distinct()
        distinct.forEachIndexed { index, trackId ->
            scheduleBlockTrackRepository.save(
                ScheduleBlockTrack.create(block = block, setlistTrackId = trackId, ordinal = index),
            )
        }
        return distinct
    }

    private fun validateTracksInPerformance(
        performanceId: UUID,
        trackIds: List<UUID>,
    ) {
        if (trackIds.isEmpty()) throw BusinessException(ErrorCode.SCHEDULE_BLOCK_TRACK_REQUIRED)
        val candidateTrackIds = candidateTrackIds(performanceId)
        if (!candidateTrackIds.containsAll(trackIds.toSet())) {
            throw BusinessException(ErrorCode.SCHEDULE_BLOCK_TRACK_NOT_IN_PERFORMANCE)
        }
    }

    private fun candidateTrackIds(performanceId: UUID): Set<UUID> {
        val performance =
            performanceRepository.findByIdOrNull(performanceId)
                ?: throw BusinessException(ErrorCode.PERFORMANCE_NOT_FOUND)
        val setlistIds = performance.setlists.map { it.setlistId }
        if (setlistIds.isEmpty()) return emptySet()
        return setlistTrackRepository.findAllBySetlistIdIn(setlistIds).map { it.id }.toSet()
    }

    private fun toRecurrenceRule(request: ScheduleBlockUpsertRequest): RecurrenceRule {
        val r = request.recurrence ?: return RecurrenceRule.none()
        return RecurrenceRule(
            freq = r.freq,
            interval = r.interval,
            until = r.until,
            count = r.count,
        )
    }

    private fun getBoardOrThrow(
        performanceId: UUID,
        boardId: UUID,
    ): ScheduleBoard {
        val board =
            scheduleBoardRepository.findByIdOrNull(boardId)
                ?: throw BusinessException(ErrorCode.SCHEDULE_BOARD_NOT_FOUND)
        if (board.performanceId != performanceId) {
            throw BusinessException(ErrorCode.SCHEDULE_BOARD_NOT_FOUND)
        }
        return board
    }

    private fun getBlockOrThrow(
        boardId: UUID,
        blockId: UUID,
    ): ScheduleBlock {
        val block =
            scheduleBlockRepository.findByIdOrNull(blockId)
                ?: throw BusinessException(ErrorCode.SCHEDULE_BLOCK_NOT_FOUND)
        if (block.board.id != boardId) {
            throw BusinessException(ErrorCode.SCHEDULE_BLOCK_NOT_FOUND)
        }
        return block
    }

    private fun ensureBoardEditable(board: ScheduleBoard) {
        if (board.confirmed) {
            throw BusinessException(ErrorCode.SCHEDULE_BOARD_ALREADY_CONFIRMED)
        }
    }

    private fun validateSlot(
        startSlot: Int,
        durationSlots: Int,
    ) {
        if (startSlot < 0 ||
            durationSlots < 1 ||
            startSlot + durationSlots > ScheduleBlock.SLOTS_PER_DAY
        ) {
            throw BusinessException(ErrorCode.SCHEDULE_SLOT_INVALID)
        }
    }

    private fun validateDateInWindow(
        date: LocalDate,
        window: PracticeWindow,
    ) {
        if (date.isBefore(window.from) || date.isAfter(window.to)) {
            throw BusinessException(ErrorCode.SCHEDULE_DATE_OUT_OF_WINDOW)
        }
    }
}
