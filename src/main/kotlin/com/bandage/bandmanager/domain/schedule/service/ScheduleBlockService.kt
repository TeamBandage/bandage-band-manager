package com.bandage.bandmanager.domain.schedule.service

import com.bandage.bandmanager.domain.schedule.dto.req.ScheduleBlockUpsertRequest
import com.bandage.bandmanager.domain.schedule.dto.res.ScheduleBlockResponse
import com.bandage.bandmanager.domain.schedule.model.RecurrenceRule
import com.bandage.bandmanager.domain.schedule.model.ScheduleBlock
import com.bandage.bandmanager.domain.schedule.model.ScheduleBlockTrack
import com.bandage.bandmanager.domain.schedule.model.ScheduleBoard
import com.bandage.bandmanager.domain.schedule.model.ScheduleWindow
import com.bandage.bandmanager.domain.schedule.model.Slot
import com.bandage.bandmanager.domain.schedule.repository.ScheduleBlockRepository
import com.bandage.bandmanager.domain.schedule.repository.ScheduleBlockTrackRepository
import com.bandage.bandmanager.domain.schedule.repository.ScheduleBoardRepository
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
    private val setlistTrackRepository: SetlistTrackRepository,
    private val scheduleAuthService: ScheduleAuthService,
) {
    @Transactional
    fun upsertBlock(
        setlistId: UUID,
        boardId: UUID,
        blockId: UUID,
        memberId: Long,
        request: ScheduleBlockUpsertRequest,
    ): ScheduleBlockResponse {
        scheduleAuthService.validateSetlistManager(setlistId, memberId)
        val board = getBoardOrThrow(setlistId, boardId)
        ensureBoardEditable(board)
        val slot = toSlot(request)
        board.scheduleWindowOrNull()?.let { validateDatesInWindow(request.startDate, request.endDate, it) }
        validateTracksInSetlist(setlistId, request.trackIds)

        val recurrence = toRecurrenceRule(request)
        val existing = scheduleBlockRepository.findByIdOrNull(blockId)
        val block =
            if (existing != null) {
                if (existing.board.id != boardId) {
                    throw BusinessException(ErrorCode.SCHEDULE_BLOCK_NOT_FOUND)
                }
                existing.reposition(slot)
                existing.updateTitle(request.title)
                existing.updateNote(request.note)
                existing.updateRecurrenceRule(recurrence)
                request.pinned?.let { if (it) existing.pin() else existing.unpin() }
                existing
            } else {
                ScheduleBlock
                    .create(
                        id = blockId,
                        board = board,
                        slot = slot,
                        title = request.title,
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
        setlistId: UUID,
        boardId: UUID,
        blockId: UUID,
        memberId: Long,
    ) {
        scheduleAuthService.validateSetlistManager(setlistId, memberId)
        val board = getBoardOrThrow(setlistId, boardId)
        ensureBoardEditable(board)
        val block = getBlockOrThrow(boardId, blockId)
        scheduleBlockTrackRepository.deleteAllByBlockId(block.id)
        scheduleBlockRepository.delete(block)
    }

    @Transactional
    fun setPin(
        setlistId: UUID,
        boardId: UUID,
        blockId: UUID,
        memberId: Long,
        pinned: Boolean,
    ): ScheduleBlockResponse {
        scheduleAuthService.validateSetlistManager(setlistId, memberId)
        val board = getBoardOrThrow(setlistId, boardId)
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
        // 삭제를 먼저 DB 에 반영한다. flush 없이 이어지는 insert 가 먼저 나가면
        // uk_schedule_block_track (schedule_block_id, setlist_track_id) 유니크 제약과 충돌한다.
        scheduleBlockTrackRepository.flush()
        val distinct = trackIds.distinct()
        distinct.forEachIndexed { index, trackId ->
            scheduleBlockTrackRepository.save(
                ScheduleBlockTrack.create(block = block, setlistTrackId = trackId, ordinal = index),
            )
        }
        return distinct
    }

    private fun validateTracksInSetlist(
        setlistId: UUID,
        trackIds: List<UUID>,
    ) {
        if (trackIds.isEmpty()) throw BusinessException(ErrorCode.SCHEDULE_BLOCK_TRACK_REQUIRED)
        val candidateTrackIds =
            setlistTrackRepository.findAllBySetlistIdIn(listOf(setlistId)).map { it.id }.toSet()
        if (!candidateTrackIds.containsAll(trackIds.toSet())) {
            throw BusinessException(ErrorCode.SCHEDULE_BLOCK_TRACK_NOT_IN_SETLIST)
        }
    }

    // 슬롯 규약 검증은 Slot init 에서 수행되므로, 생성 실패 시 BusinessException 으로 변환한다.
    private fun toSlot(request: ScheduleBlockUpsertRequest): Slot =
        try {
            Slot.of(request.startDate, request.startSlot, request.endDate, request.endSlot)
        } catch (e: IllegalArgumentException) {
            throw BusinessException(ErrorCode.INVALID_SLOT_RANGE)
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
        setlistId: UUID,
        boardId: UUID,
    ): ScheduleBoard {
        val board =
            scheduleBoardRepository.findByIdOrNull(boardId)
                ?: throw BusinessException(ErrorCode.SCHEDULE_BOARD_NOT_FOUND)
        if (board.setlistId != setlistId) {
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

    private fun validateDatesInWindow(
        startDate: LocalDate,
        endDate: LocalDate,
        window: ScheduleWindow,
    ) {
        if (startDate.isBefore(window.from) || endDate.isAfter(window.to)) {
            throw BusinessException(ErrorCode.SCHEDULE_DATE_OUT_OF_WINDOW)
        }
    }
}
