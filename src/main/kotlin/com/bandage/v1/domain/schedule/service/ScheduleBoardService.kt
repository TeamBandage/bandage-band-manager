package com.bandage.v1.domain.schedule.service

import com.bandage.v1.domain.schedule.dto.req.ScheduleBoardCreateRequest
import com.bandage.v1.domain.schedule.dto.req.ScheduleBoardUpdateRequest
import com.bandage.v1.domain.schedule.dto.res.ScheduleBoardResponse
import com.bandage.v1.domain.schedule.model.ScheduleBoard
import com.bandage.v1.domain.schedule.model.ScheduleBoardConstraints
import com.bandage.v1.domain.schedule.repository.ScheduleBlockRepository
import com.bandage.v1.domain.schedule.repository.ScheduleBoardRepository
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ScheduleBoardService(
    private val scheduleBoardRepository: ScheduleBoardRepository,
    private val scheduleBlockRepository: ScheduleBlockRepository,
    private val scheduleAuthService: ScheduleAuthService,
) {
    fun getBoards(
        meetingId: UUID,
        memberId: Long,
    ): List<ScheduleBoardResponse> {
        scheduleAuthService.validateParticipant(meetingId, memberId)
        val boards = scheduleBoardRepository.findAllByMeetingId(meetingId)
        if (boards.isEmpty()) return emptyList()
        val boardIds = boards.map { it.id }
        val blocksByBoard =
            boardIds
                .associateWith { scheduleBlockRepository.findAllByBoardId(it) }
        return boards.map { ScheduleBoardResponse.of(it, blocksByBoard[it.id].orEmpty()) }
    }

    @Transactional
    fun createBoard(
        meetingId: UUID,
        memberId: Long,
        request: ScheduleBoardCreateRequest,
    ): ScheduleBoardResponse {
        scheduleAuthService.validateManager(meetingId, memberId)
        val existingCount = scheduleBoardRepository.findAllByMeetingId(meetingId).size
        if (existingCount >= MAX_BOARDS_PER_MEETING) {
            throw BusinessException(ErrorCode.SCHEDULE_BOARD_LIMIT_EXCEEDED)
        }
        val constraints = request.constraints?.toEntity() ?: ScheduleBoardConstraints()
        val board =
            scheduleBoardRepository.save(
                ScheduleBoard.create(
                    meetingId = meetingId,
                    name = request.name,
                    paletteSeed = request.paletteSeed,
                    constraints = constraints,
                ),
            )
        return ScheduleBoardResponse.of(board, emptyList())
    }

    @Transactional
    fun updateBoard(
        meetingId: UUID,
        boardId: UUID,
        memberId: Long,
        request: ScheduleBoardUpdateRequest,
    ): ScheduleBoardResponse {
        scheduleAuthService.validateManager(meetingId, memberId)
        val board = getBoardOrThrow(meetingId, boardId)
        if (board.confirmed) {
            throw BusinessException(ErrorCode.SCHEDULE_BOARD_ALREADY_CONFIRMED)
        }
        request.name?.let { board.rename(it) }
        if (request.paletteSeed != null) board.updatePaletteSeed(request.paletteSeed)
        request.constraints?.let { dto ->
            board.constraints.update(
                workingHoursStart = dto.workingHoursStart,
                workingHoursEnd = dto.workingHoursEnd,
                excludeLateNight = dto.excludeLateNight,
                maxConsecutiveMinutes = dto.maxConsecutiveMinutes,
            )
        }
        val saved =
            try {
                scheduleBoardRepository.saveAndFlush(board)
            } catch (e: ObjectOptimisticLockingFailureException) {
                throw BusinessException(ErrorCode.SCHEDULE_BOARD_VERSION_CONFLICT)
            }
        val blocks = scheduleBlockRepository.findAllByBoardId(saved.id)
        return ScheduleBoardResponse.of(saved, blocks)
    }

    @Transactional
    fun deleteBoard(
        meetingId: UUID,
        boardId: UUID,
        memberId: Long,
    ) {
        scheduleAuthService.validateManager(meetingId, memberId)
        val board = getBoardOrThrow(meetingId, boardId)
        if (board.confirmed) {
            throw BusinessException(ErrorCode.SCHEDULE_BOARD_ALREADY_CONFIRMED)
        }
        scheduleBoardRepository.delete(board)
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

    companion object {
        const val MAX_BOARDS_PER_MEETING = 5
    }
}
