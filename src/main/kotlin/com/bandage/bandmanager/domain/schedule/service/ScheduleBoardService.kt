package com.bandage.bandmanager.domain.schedule.service

import com.bandage.bandmanager.domain.schedule.dto.req.ScheduleBoardCreateRequest
import com.bandage.bandmanager.domain.schedule.dto.req.ScheduleBoardUpdateRequest
import com.bandage.bandmanager.domain.schedule.dto.res.ScheduleBoardResponse
import com.bandage.bandmanager.domain.schedule.model.ScheduleBoard
import com.bandage.bandmanager.domain.schedule.model.ScheduleBoardConstraints
import com.bandage.bandmanager.domain.schedule.repository.ScheduleBlockRepository
import com.bandage.bandmanager.domain.schedule.repository.ScheduleBlockTrackRepository
import com.bandage.bandmanager.domain.schedule.repository.ScheduleBoardRepository
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ScheduleBoardService(
    private val scheduleBoardRepository: ScheduleBoardRepository,
    private val scheduleBlockRepository: ScheduleBlockRepository,
    private val scheduleBlockTrackRepository: ScheduleBlockTrackRepository,
    private val scheduleAuthService: ScheduleAuthService,
) {
    fun getBoards(
        setlistId: UUID,
        memberId: Long,
    ): List<ScheduleBoardResponse> {
        scheduleAuthService.validateSetlistParticipant(setlistId, memberId)
        val boards = scheduleBoardRepository.findAllBySetlistId(setlistId)
        if (boards.isEmpty()) return emptyList()
        val blocksByBoard = boards.associate { it.id to scheduleBlockRepository.findAllByBoardId(it.id) }
        val allBlockIds = blocksByBoard.values.flatten().map { it.id }
        val trackIdsByBlock = trackIdsByBlock(allBlockIds)
        return boards.map { board ->
            ScheduleBoardResponse.of(board, blocksByBoard[board.id].orEmpty(), trackIdsByBlock)
        }
    }

    @Transactional
    fun createBoard(
        setlistId: UUID,
        memberId: Long,
        request: ScheduleBoardCreateRequest,
    ): ScheduleBoardResponse {
        scheduleAuthService.validateSetlistManager(setlistId, memberId)
        val existingCount = scheduleBoardRepository.findAllBySetlistId(setlistId).size
        if (existingCount >= MAX_BOARDS_PER_SETLIST) {
            throw BusinessException(ErrorCode.SCHEDULE_BOARD_LIMIT_EXCEEDED)
        }
        val constraints = request.constraints?.toEntity() ?: ScheduleBoardConstraints()
        val board =
            scheduleBoardRepository.save(
                ScheduleBoard.create(
                    setlistId = setlistId,
                    name = request.name,
                    constraints = constraints,
                    windowFrom = request.windowFrom,
                    windowTo = request.windowTo,
                ),
            )
        return ScheduleBoardResponse.of(board, emptyList())
    }

    @Transactional
    fun updateBoard(
        setlistId: UUID,
        boardId: UUID,
        memberId: Long,
        request: ScheduleBoardUpdateRequest,
    ): ScheduleBoardResponse {
        scheduleAuthService.validateSetlistManager(setlistId, memberId)
        val board = getBoardOrThrow(setlistId, boardId)
        if (board.confirmed) {
            throw BusinessException(ErrorCode.SCHEDULE_BOARD_ALREADY_CONFIRMED)
        }
        request.name?.let { board.rename(it) }
        if (request.windowFrom != null || request.windowTo != null) {
            board.updateWindow(request.windowFrom ?: board.windowFrom, request.windowTo ?: board.windowTo)
        }
        request.constraints?.let { dto ->
            board.constraints.update(
                workingHoursStart = dto.workingHoursStart,
                workingHoursEnd = dto.workingHoursEnd,
                excludeLateNight = dto.excludeLateNight,
                maxConsecutiveMinutes = dto.maxConsecutiveMinutes,
            )
        }
        val blocks = scheduleBlockRepository.findAllByBoardId(board.id)
        val trackIdsByBlock = trackIdsByBlock(blocks.map { it.id })
        return ScheduleBoardResponse.of(board, blocks, trackIdsByBlock)
    }

    @Transactional
    fun deleteBoard(
        setlistId: UUID,
        boardId: UUID,
        memberId: Long,
    ) {
        scheduleAuthService.validateSetlistManager(setlistId, memberId)
        val board = getBoardOrThrow(setlistId, boardId)
        if (board.confirmed) {
            throw BusinessException(ErrorCode.SCHEDULE_BOARD_ALREADY_CONFIRMED)
        }
        scheduleBoardRepository.delete(board)
    }

    private fun trackIdsByBlock(blockIds: List<UUID>): Map<UUID, List<UUID>> {
        if (blockIds.isEmpty()) return emptyMap()
        return scheduleBlockTrackRepository
            .findAllByBlockIdIn(blockIds)
            .sortedBy { it.ordinal }
            .groupBy({ it.block.id }, { it.setlistTrackId })
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

    companion object {
        const val MAX_BOARDS_PER_SETLIST = 5
    }
}
