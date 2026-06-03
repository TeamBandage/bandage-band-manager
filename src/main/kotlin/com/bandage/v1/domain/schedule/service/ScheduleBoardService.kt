package com.bandage.v1.domain.schedule.service

import com.bandage.v1.domain.schedule.dto.req.ScheduleBoardCreateRequest
import com.bandage.v1.domain.schedule.dto.req.ScheduleBoardUpdateRequest
import com.bandage.v1.domain.schedule.dto.res.ScheduleBoardResponse
import com.bandage.v1.domain.schedule.model.ScheduleBoard
import com.bandage.v1.domain.schedule.model.ScheduleBoardConstraints
import com.bandage.v1.domain.schedule.repository.ScheduleBlockRepository
import com.bandage.v1.domain.schedule.repository.ScheduleBlockTrackRepository
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
    private val scheduleBlockTrackRepository: ScheduleBlockTrackRepository,
    private val scheduleAuthService: ScheduleAuthService,
) {
    fun getBoards(
        performanceId: UUID,
        memberId: Long,
    ): List<ScheduleBoardResponse> {
        scheduleAuthService.validatePerformanceParticipant(performanceId, memberId)
        val boards = scheduleBoardRepository.findAllByPerformanceId(performanceId)
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
        performanceId: UUID,
        memberId: Long,
        request: ScheduleBoardCreateRequest,
    ): ScheduleBoardResponse {
        scheduleAuthService.validatePerformanceManager(performanceId, memberId)
        val existingCount = scheduleBoardRepository.findAllByPerformanceId(performanceId).size
        if (existingCount >= MAX_BOARDS_PER_PERFORMANCE) {
            throw BusinessException(ErrorCode.SCHEDULE_BOARD_LIMIT_EXCEEDED)
        }
        val constraints = request.constraints?.toEntity() ?: ScheduleBoardConstraints()
        val board =
            scheduleBoardRepository.save(
                ScheduleBoard.create(
                    performanceId = performanceId,
                    name = request.name,
                    paletteSeed = request.paletteSeed,
                    constraints = constraints,
                    windowFrom = request.windowFrom,
                    windowTo = request.windowTo,
                ),
            )
        return ScheduleBoardResponse.of(board, emptyList())
    }

    @Transactional
    fun updateBoard(
        performanceId: UUID,
        boardId: UUID,
        memberId: Long,
        request: ScheduleBoardUpdateRequest,
    ): ScheduleBoardResponse {
        scheduleAuthService.validatePerformanceManager(performanceId, memberId)
        val board = getBoardOrThrow(performanceId, boardId)
        if (board.confirmed) {
            throw BusinessException(ErrorCode.SCHEDULE_BOARD_ALREADY_CONFIRMED)
        }
        request.name?.let { board.rename(it) }
        if (request.paletteSeed != null) board.updatePaletteSeed(request.paletteSeed)
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
        val saved =
            try {
                scheduleBoardRepository.saveAndFlush(board)
            } catch (e: ObjectOptimisticLockingFailureException) {
                throw BusinessException(ErrorCode.SCHEDULE_BOARD_VERSION_CONFLICT)
            }
        val blocks = scheduleBlockRepository.findAllByBoardId(saved.id)
        val trackIdsByBlock = trackIdsByBlock(blocks.map { it.id })
        return ScheduleBoardResponse.of(saved, blocks, trackIdsByBlock)
    }

    @Transactional
    fun deleteBoard(
        performanceId: UUID,
        boardId: UUID,
        memberId: Long,
    ) {
        scheduleAuthService.validatePerformanceManager(performanceId, memberId)
        val board = getBoardOrThrow(performanceId, boardId)
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

    companion object {
        const val MAX_BOARDS_PER_PERFORMANCE = 5
    }
}
