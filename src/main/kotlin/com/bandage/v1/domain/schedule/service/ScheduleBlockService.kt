package com.bandage.v1.domain.schedule.service

import com.bandage.v1.domain.schedule.dto.req.ScheduleBlockUpsertRequest
import com.bandage.v1.domain.schedule.dto.res.ScheduleBlockResponse
import com.bandage.v1.domain.schedule.model.ScheduleBlock
import com.bandage.v1.domain.schedule.model.ScheduleBoard
import com.bandage.v1.domain.schedule.repository.ScheduleBlockRepository
import com.bandage.v1.domain.schedule.repository.ScheduleBoardRepository
import com.bandage.v1.domain.setlist.model.PracticeWindow
import com.bandage.v1.domain.setlist.repository.SetlistMeetingRepository
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
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
    private val setlistMeetingRepository: SetlistMeetingRepository,
    private val scheduleAuthService: ScheduleAuthService,
) {
    @Transactional
    fun upsertBlock(
        meetingId: UUID,
        boardId: UUID,
        blockId: UUID,
        memberId: Long,
        request: ScheduleBlockUpsertRequest,
    ): ScheduleBlockResponse {
        scheduleAuthService.validateManager(meetingId, memberId)
        val board = getBoardOrThrow(meetingId, boardId)
        ensureBoardEditable(board)
        validateSlot(request.startSlot, request.durationSlots)
        val window = getPracticeWindow(meetingId)
        validateDateInWindow(request.date, window)

        val existing = scheduleBlockRepository.findByIdOrNull(blockId)
        val block =
            if (existing != null) {
                if (existing.board.id != boardId) {
                    throw BusinessException(ErrorCode.SCHEDULE_BLOCK_NOT_FOUND)
                }
                existing.reposition(request.date, request.startSlot, request.durationSlots)
                existing.updatePaletteIndex(request.paletteIndex)
                existing.updateSongTitleOverride(request.songTitleOverride)
                existing.updateNote(request.note)
                request.pinned?.let { if (it) existing.pin() else existing.unpin() }
                existing
            } else {
                ScheduleBlock
                    .create(
                        id = blockId,
                        board = board,
                        songId = request.songId,
                        date = request.date,
                        startSlot = request.startSlot,
                        durationSlots = request.durationSlots,
                        paletteIndex = request.paletteIndex,
                        songTitleOverride = request.songTitleOverride,
                        note = request.note,
                    ).apply {
                        request.pinned?.let { if (it) pin() }
                    }
            }
        val saved = scheduleBlockRepository.save(block)
        return ScheduleBlockResponse.from(saved)
    }

    @Transactional
    fun deleteBlock(
        meetingId: UUID,
        boardId: UUID,
        blockId: UUID,
        memberId: Long,
    ) {
        scheduleAuthService.validateManager(meetingId, memberId)
        val board = getBoardOrThrow(meetingId, boardId)
        ensureBoardEditable(board)
        val block = getBlockOrThrow(boardId, blockId)
        scheduleBlockRepository.delete(block)
    }

    @Transactional
    fun setPin(
        meetingId: UUID,
        boardId: UUID,
        blockId: UUID,
        memberId: Long,
        pinned: Boolean,
    ): ScheduleBlockResponse {
        scheduleAuthService.validateManager(meetingId, memberId)
        val board = getBoardOrThrow(meetingId, boardId)
        ensureBoardEditable(board)
        val block = getBlockOrThrow(boardId, blockId)
        if (pinned) block.pin() else block.unpin()
        return ScheduleBlockResponse.from(block)
    }

    private fun getBoardOrThrow(
        meetingId: UUID,
        boardId: UUID,
    ): ScheduleBoard {
        val board =
            scheduleBoardRepository.findByIdOrNull(boardId)
                ?: throw BusinessException(ErrorCode.SCHEDULE_BOARD_NOT_FOUND)
        if (board.meetingId != meetingId) {
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

    private fun getPracticeWindow(meetingId: UUID): PracticeWindow {
        val meeting =
            setlistMeetingRepository.findByIdOrNull(meetingId)
                ?: throw BusinessException(ErrorCode.SETLIST_MEETING_NOT_FOUND)
        return meeting.practiceWindow
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
