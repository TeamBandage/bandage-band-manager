package com.bandage.bandmanager.facade

import com.bandage.bandmanager.domain.jam.model.Jam
import com.bandage.bandmanager.domain.jam.repository.JamRepository
import com.bandage.bandmanager.domain.jam.service.JamReservationSyncService
import com.bandage.bandmanager.domain.jam.service.SetlistTrackToJamConverter
import com.bandage.bandmanager.domain.schedule.dto.res.ScheduleConfirmResponse
import com.bandage.bandmanager.domain.schedule.dto.res.ScheduleUnconfirmResponse
import com.bandage.bandmanager.domain.schedule.model.RecurrenceFreq
import com.bandage.bandmanager.domain.schedule.model.ScheduleBlock
import com.bandage.bandmanager.domain.schedule.model.ScheduleBlockJam
import com.bandage.bandmanager.domain.schedule.model.ScheduleBoard
import com.bandage.bandmanager.domain.schedule.repository.ScheduleBlockJamRepository
import com.bandage.bandmanager.domain.schedule.repository.ScheduleBlockRepository
import com.bandage.bandmanager.domain.schedule.repository.ScheduleBlockTrackRepository
import com.bandage.bandmanager.domain.schedule.repository.ScheduleBoardRepository
import com.bandage.bandmanager.domain.schedule.service.ScheduleAuthService
import com.bandage.bandmanager.domain.setlist.model.SetlistTrack
import com.bandage.bandmanager.domain.setlist.repository.SetlistTrackParticipantRepository
import com.bandage.bandmanager.domain.setlist.repository.SetlistTrackRepository
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * 시간표 시안(ScheduleBoard) 확정/해제 Facade. PRD-2 에서 Performance 스코프로 재작성됨.
 *
 * 확정 시 각 ScheduleBlock 을 (반복 전개된) 발생일 × 트랙 단위로 [SetlistTrackToJamConverter] 를 통해
 * Jam 으로 생성하고, 어떤 블록이 어떤 Jam 을 만들었는지 [ScheduleBlockJam] 으로 추적한다.
 * Jam 은 단일 트랙(TrackInfo)을 가지므로 한 블록의 여러 트랙은 같은 시간대에 트랙별 Jam 으로 분리 생성된다.
 */
@Service
class ScheduleConfirmFacade(
    private val scheduleBoardRepository: ScheduleBoardRepository,
    private val scheduleBlockRepository: ScheduleBlockRepository,
    private val scheduleBlockTrackRepository: ScheduleBlockTrackRepository,
    private val scheduleBlockJamRepository: ScheduleBlockJamRepository,
    private val setlistTrackRepository: SetlistTrackRepository,
    private val setlistTrackParticipantRepository: SetlistTrackParticipantRepository,
    private val setlistTrackToJamConverter: SetlistTrackToJamConverter,
    private val jamRepository: JamRepository,
    private val jamReservationSyncService: JamReservationSyncService,
    private val scheduleAuthService: ScheduleAuthService,
) {
    @Transactional
    fun confirmBoard(
        performanceId: UUID,
        boardId: UUID,
        memberId: Long,
    ): ScheduleConfirmResponse {
        scheduleAuthService.validatePerformanceManager(performanceId, memberId)
        val board = getBoardOrThrow(performanceId, boardId)

        val samePerformanceBoards = scheduleBoardRepository.findAllByPerformanceId(performanceId)
        if (samePerformanceBoards.any { it.id != board.id && it.confirmed }) {
            throw BusinessException(ErrorCode.SCHEDULE_BOARD_ALREADY_CONFIRMED)
        }

        val blocks = scheduleBlockRepository.findAllByBoardId(boardId)

        // 블록별 트랙 매핑 및 SetlistTrack/참여자 일괄 로드
        val blockTracks =
            scheduleBlockTrackRepository
                .findAllByBlockIdIn(blocks.map { it.id })
                .groupBy({ it.block.id }, { it.setlistTrackId to it.ordinal })
        val allTrackIds =
            blockTracks.values
                .flatten()
                .map { it.first }
                .distinct()
        val tracksById = setlistTrackRepository.findAllById(allTrackIds).associateBy { it.id }
        val participantsByTrack =
            setlistTrackParticipantRepository
                .findAllByTrackIn(tracksById.values.toList())
                .groupBy { it.track.id }

        val createdJams = mutableListOf<Jam>()
        blocks.forEach { block ->
            val orderedTrackIds =
                blockTracks[block.id].orEmpty().sortedBy { it.second }.map { it.first }
            val occurrences = expandRecurrence(block, board.windowTo)
            occurrences.forEach { occurrenceDate ->
                val startAt = startAtOf(occurrenceDate, block.startSlot)
                val durationMinutes = block.durationSlots * MINUTES_PER_SLOT
                orderedTrackIds.forEach { trackId ->
                    val track: SetlistTrack =
                        tracksById[trackId]
                            ?: throw BusinessException(ErrorCode.SETLIST_TRACK_NOT_FOUND)
                    val jam =
                        setlistTrackToJamConverter.toJam(
                            track = track,
                            participants = participantsByTrack[trackId].orEmpty(),
                            startAt = startAt,
                            durationMinutes = durationMinutes,
                            venue = null,
                            titleOverride = block.titleOverride,
                        )
                    scheduleBlockJamRepository.save(
                        ScheduleBlockJam.create(
                            scheduleBlockId = block.id,
                            scheduleBoardId = board.id,
                            jamId = jam.id,
                        ),
                    )
                    createdJams.add(jam)
                }
            }
        }

        board.confirm()
        return ScheduleConfirmResponse(
            confirmedAt = LocalDateTime.now(),
            jamsCreated =
                createdJams.map {
                    ScheduleConfirmResponse.JamCreatedSummary(
                        jamId = it.id,
                        title = it.title,
                        startAt = it.timeInfo.startAt,
                        durationMinutes = it.timeInfo.durationMinutes,
                    )
                },
        )
    }

    @Transactional
    fun unconfirmBoard(
        performanceId: UUID,
        boardId: UUID,
        memberId: Long,
    ): ScheduleUnconfirmResponse {
        scheduleAuthService.validatePerformanceManager(performanceId, memberId)
        val board = getBoardOrThrow(performanceId, boardId)
        if (!board.confirmed) {
            throw BusinessException(ErrorCode.SCHEDULE_BOARD_NOT_CONFIRMED)
        }

        // 확정 시 생성된 Jam 을 정리(soft-delete + 예약 동기화)하고 추적 매핑 삭제
        val mappings = scheduleBlockJamRepository.findAllByScheduleBoardId(boardId)
        val jamIds = mappings.map { it.jamId }.distinct()
        jamRepository.findAllById(jamIds).forEach { jam ->
            jam.markAsDeleted(memberId)
            jamReservationSyncService.sync(jam)
        }
        scheduleBlockJamRepository.deleteAllByScheduleBoardId(boardId)

        board.unconfirm()
        return ScheduleUnconfirmResponse(unconfirmedAt = LocalDateTime.now())
    }

    /**
     * 반복 블록을 발생일 목록으로 전개한다.
     * 단발성(NONE)이면 [block.date] 1건. 반복이면 until → boardWindowTo → count 순으로 경계를 적용하며,
     * 경계가 전혀 없으면 무한 전개를 막기 위해 단발성으로 간주한다.
     */
    fun expandRecurrence(
        block: ScheduleBlock,
        boardWindowTo: LocalDate?,
    ): List<LocalDate> {
        val rule = block.recurrenceRule
        if (!rule.isRecurring) return listOf(block.date)

        val stepDays =
            when (rule.freq) {
                RecurrenceFreq.DAILY -> 1
                RecurrenceFreq.WEEKLY -> 7
                RecurrenceFreq.BIWEEKLY -> 14
                RecurrenceFreq.NONE -> return listOf(block.date)
            } * rule.interval.coerceAtLeast(1)

        val hardEnd = rule.until ?: boardWindowTo
        val maxCount = rule.count ?: Int.MAX_VALUE
        if (hardEnd == null && rule.count == null) return listOf(block.date)

        val result = mutableListOf<LocalDate>()
        var cur = block.date
        while (result.size < SAFETY_CAP && result.size < maxCount) {
            if (hardEnd != null && cur.isAfter(hardEnd)) break
            result.add(cur)
            cur = cur.plusDays(stepDays.toLong())
        }
        return result.ifEmpty { listOf(block.date) }
    }

    private fun startAtOf(
        date: LocalDate,
        startSlot: Int,
    ): LocalDateTime = date.atStartOfDay().plusMinutes(startSlot.toLong() * MINUTES_PER_SLOT)

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
        private const val MINUTES_PER_SLOT: Int = 30
        private const val SAFETY_CAP: Int = 366
    }
}
