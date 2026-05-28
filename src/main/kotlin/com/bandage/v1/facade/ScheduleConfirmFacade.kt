package com.bandage.v1.facade

import com.bandage.v1.domain.practice.model.Practice
import com.bandage.v1.domain.practice.model.PracticeSong
import com.bandage.v1.domain.practice.repository.PracticeRepository
import com.bandage.v1.domain.practice.repository.PracticeSongRepository
import com.bandage.v1.domain.schedule.dto.res.ScheduleConfirmResponse
import com.bandage.v1.domain.schedule.dto.res.ScheduleUnconfirmResponse
import com.bandage.v1.domain.schedule.model.ScheduleBlock
import com.bandage.v1.domain.schedule.model.ScheduleBoard
import com.bandage.v1.domain.schedule.repository.ScheduleBlockRepository
import com.bandage.v1.domain.schedule.repository.ScheduleBoardRepository
import com.bandage.v1.domain.schedule.service.ScheduleAuthService
import com.bandage.v1.domain.selection.model.TrackSelection
import com.bandage.v1.domain.selection.model.TrackSelectionItem
import com.bandage.v1.domain.selection.repository.TrackSelectionItemRepository
import com.bandage.v1.domain.selection.repository.TrackSelectionMemberRepository
import com.bandage.v1.domain.selection.repository.TrackSelectionRepository
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class ScheduleConfirmFacade(
    private val scheduleBoardRepository: ScheduleBoardRepository,
    private val scheduleBlockRepository: ScheduleBlockRepository,
    private val trackSelectionRepository: TrackSelectionRepository,
    private val trackSelectionMemberRepository: TrackSelectionMemberRepository,
    private val trackSelectionItemRepository: TrackSelectionItemRepository,
    private val practiceSongRepository: PracticeSongRepository,
    private val practiceRepository: PracticeRepository,
    private val scheduleAuthService: ScheduleAuthService,
) {
    @Transactional
    fun confirmBoard(
        meetingId: UUID,
        boardId: UUID,
        memberId: Long,
    ): ScheduleConfirmResponse {
        val board = getBoardOrThrow(meetingId, boardId)
        val meeting = getMeetingOrThrow(meetingId)
        scheduleAuthService.validateManager(meetingId, memberId)
        if (!meeting.isLocked) throw BusinessException(ErrorCode.SETLIST_NOT_LOCKED)

        val sameMeetingBoards = scheduleBoardRepository.findAllByMeetingId(meetingId)
        if (sameMeetingBoards.any { it.id != board.id && it.confirmed }) {
            throw BusinessException(ErrorCode.SCHEDULE_BOARD_ALREADY_CONFIRMED)
        }

        val blocks = scheduleBlockRepository.findAllByBoardId(boardId)
        val items = trackSelectionItemRepository.findAllBySelection(meeting).associateBy { it.id }
        val participantIds = trackSelectionMemberRepository.findAllBySelectionId(meetingId).map { it.memberId }

        val createdPractices =
            blocks.map { block ->
                val item = items[block.songId] ?: throw BusinessException(ErrorCode.SETLIST_MEETING_ITEM_NOT_FOUND)
                val practiceSong = resolvePracticeSong(item)
                val practice = buildPractice(block, practiceSong)
                participantIds.forEach { practice.addParticipant(it) }
                practiceRepository.save(practice)
            }

        board.confirm()
        val confirmedAt = LocalDateTime.now()

        return ScheduleConfirmResponse(
            confirmedAt = confirmedAt,
            practicesCreated =
                createdPractices.map {
                    ScheduleConfirmResponse.PracticeCreatedSummary(
                        practiceId = it.id,
                        title = it.title,
                        startAt = it.timeInfo.startAt,
                        durationMinutes = it.timeInfo.durationMinutes,
                    )
                },
        )
    }

    @Transactional
    fun unconfirmBoard(
        meetingId: UUID,
        boardId: UUID,
        memberId: Long,
    ): ScheduleUnconfirmResponse {
        scheduleAuthService.validateManager(meetingId, memberId)
        val board = getBoardOrThrow(meetingId, boardId)
        if (!board.confirmed) {
            throw BusinessException(ErrorCode.SCHEDULE_BOARD_NOT_CONFIRMED)
        }
        board.unconfirm()
        return ScheduleUnconfirmResponse(unconfirmedAt = LocalDateTime.now())
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

    private fun getMeetingOrThrow(meetingId: UUID): TrackSelection =
        trackSelectionRepository.findByIdOrNull(meetingId)
            ?: throw BusinessException(ErrorCode.SETLIST_MEETING_NOT_FOUND)

    private fun resolvePracticeSong(item: TrackSelectionItem): PracticeSong {
        val practiceSongId =
            item.practiceSongId
                ?: throw BusinessException(ErrorCode.PRACTICE_SONG_NOT_FOUND)
        return practiceSongRepository.findByIdOrNull(practiceSongId)
            ?: throw BusinessException(ErrorCode.PRACTICE_SONG_NOT_FOUND)
    }

    private fun buildPractice(
        block: ScheduleBlock,
        song: PracticeSong,
    ): Practice {
        val startAt = block.date.atStartOfDay().plusMinutes(block.startSlot.toLong() * MINUTES_PER_SLOT)
        val durationMinutes = block.durationSlots * MINUTES_PER_SLOT
        val title = block.songTitleOverride?.takeIf { it.isNotBlank() } ?: song.title
        return Practice.create(
            title = title,
            song = song,
            startAt = startAt,
            durationMinutes = durationMinutes,
            venue = null,
        )
    }

    companion object {
        private const val MINUTES_PER_SLOT: Int = 30
    }
}
