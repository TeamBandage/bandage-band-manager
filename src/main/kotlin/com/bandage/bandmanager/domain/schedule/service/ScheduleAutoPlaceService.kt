package com.bandage.bandmanager.domain.schedule.service

import com.bandage.bandmanager.domain.schedule.dto.req.ScheduleAutoPlaceRequest
import com.bandage.bandmanager.domain.schedule.dto.res.ScheduleBoardResponse
import com.bandage.bandmanager.domain.schedule.model.ScheduleBlock
import com.bandage.bandmanager.domain.schedule.model.ScheduleBlockTrack
import com.bandage.bandmanager.domain.schedule.model.ScheduleBoard
import com.bandage.bandmanager.domain.schedule.model.Slot
import com.bandage.bandmanager.domain.schedule.model.enums.Frequency
import com.bandage.bandmanager.domain.schedule.model.enums.PlacementOrigin
import com.bandage.bandmanager.domain.schedule.repository.ScheduleBlockRepository
import com.bandage.bandmanager.domain.schedule.repository.ScheduleBlockTrackRepository
import com.bandage.bandmanager.domain.schedule.repository.ScheduleBoardRepository
import com.bandage.bandmanager.domain.setlist.repository.SetlistTrackParticipantRepository
import com.bandage.bandmanager.domain.setlist.repository.SetlistTrackRepository
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.UUID

/**
 * 시간표 자동 배치.
 *
 * - 블록 = 트랙 1개. 참여 멤버 집합이 같은 트랙들(그룹)은 같은 날 인접 슬롯에 연속 배치한다.
 * - 실효 시간대 = boardTimeRange ∩ TimePreference (둘 다 하드 제약).
 *   그룹이 하루에 다 들어가지 않으면 하루 최대치(실효 폭 / jamDurationSlots)로 잘라
 *   조각별로 다른 날에 배치한다.
 * - 회차: 윈도우를 달력 기준(interval)으로 쪼갠 구간. 조각은 회차당 최대 1회 배치되고,
 *   실패한 회차는 건너뛴다. 부분 회차도 1회차로 인정한다.
 * - dayPreference 는 소프트 제약: 선호 요일에 후보가 없으면(fellBack) 다른 요일까지 연다.
 *   폴백된 조각을 먼저, 그다음 후보가 적은 조각부터 배치하고,
 *   자리는 다른 조각의 후보를 가장 적게 줄이는 위치(동률이면 이른 시간)를 고른다.
 * - pinned 블록과 그 트랙은 보존하고 멤버 점유로만 반영한다. pinned = false 블록은 배치 전에
 *   삭제한다(자식 먼저, flush 후 재생성 — uk 제약 순서 때문).
 * - maxJamsPerDay / maxEmptySlotsBetweenJams 는 멤버별 "모임"(연속 구간은 병합해 1회) 단위로 판정한다.
 * - 배치 현황은 적재하지 않는다. ScheduleBlockTrack 집계(countPlacementsByBoardId)가 조회를 대신한다.
 */
@Service
@Transactional(readOnly = true)
class ScheduleAutoPlaceService(
    private val scheduleBoardRepository: ScheduleBoardRepository,
    private val scheduleBlockRepository: ScheduleBlockRepository,
    private val scheduleBlockTrackRepository: ScheduleBlockTrackRepository,
    private val setlistTrackRepository: SetlistTrackRepository,
    private val setlistTrackParticipantRepository: SetlistTrackParticipantRepository,
    private val scheduleAvailabilityService: ScheduleAvailabilityService,
    private val scheduleAuthService: ScheduleAuthService,
) {
    @Transactional
    fun autoPlaceScheduleBlocks(
        setlistId: UUID,
        boardId: UUID,
        memberId: Long,
        request: ScheduleAutoPlaceRequest,
    ): ScheduleBoardResponse {
        scheduleAuthService.validateSetlistManager(setlistId, memberId)
        val board = getBoardOrThrow(setlistId, boardId)
        if (board.confirmed) throw BusinessException(ErrorCode.SCHEDULE_BOARD_ALREADY_CONFIRMED)
        request.validateTimePreference()
        val window = board.scheduleWindowOrNull() ?: throw BusinessException(ErrorCode.SCHEDULE_WINDOW_REQUIRED)

        val effFrom = maxOf(board.boardTimeRangeFrom, request.startTimePreference)
        val effTo = minOf(board.boardTimeRangeTo, request.endTimePreference)
        val maxTracksPerDay = if (effTo > effFrom) (effTo - effFrom) / request.jamDurationSlots else 0
        val tracks = setlistTrackRepository.findAllBySetlistIdIn(listOf(setlistId))
        if (tracks.isEmpty() || maxTracksPerDay == 0) throw BusinessException(ErrorCode.SCHEDULE_NO_PLACEABLE_TRACK)

        val membersByTrack: Map<UUID, Set<Long>> =
            setlistTrackParticipantRepository
                .findAllBySetlistId(setlistId)
                .groupBy({ it.track.id }, { it.memberId })
                .mapValues { (_, members) -> members.toSet() }

        val pinnedBlocks = scheduleBlockRepository.findAllByBoardIdAndPinned(boardId, true)
        val pinnedTrackIdsByBlock =
            scheduleBlockTrackRepository
                .findAllByBlockIdIn(pinnedBlocks.map { it.id })
                .groupBy({ it.block.id }, { it.setlistTrackId })
        val pinnedTrackIds = pinnedTrackIdsByBlock.values.flatten().toSet()

        // 고정 블록에 이미 올라간 트랙은 재배치하지 않는다
        val pieces =
            tracks
                .filter { it.id !in pinnedTrackIds }
                .groupBy { membersByTrack[it.id].orEmpty() }
                .flatMap { (members, grouped) ->
                    grouped.map { it.id }.chunked(maxTracksPerDay).map { chunk ->
                        Piece(chunk, members, chunk.size * request.jamDurationSlots)
                    }
                }

        val index =
            scheduleAvailabilityService.buildIndex(
                membersByTrack.values.flatten().distinct(),
                window.from,
                window.to,
            )
        val occupancy = Occupancy(request.maxJamsPerDay, request.maxEmptySlotsBetweenJams)
        pinnedBlocks.forEach { block ->
            val members = pinnedTrackIdsByBlock[block.id].orEmpty().flatMap { membersByTrack[it].orEmpty() }.toSet()
            occupancy.occupy(block.slot, members)
        }

        deleteStaleBlocks(boardId)

        val newBlocks = mutableListOf<ScheduleBlock>()
        val newBlockTracks = mutableListOf<ScheduleBlockTrack>()
        rounds(window.from, window.to, request.interval).forEach { (roundFrom, roundTo) ->
            placeRound(pieces, roundFrom, roundTo, effFrom, effTo, request, index, occupancy, board, newBlocks, newBlockTracks)
        }
        scheduleBlockRepository.saveAll(newBlocks)
        scheduleBlockTrackRepository.saveAll(newBlockTracks)

        val blocks = (pinnedBlocks + newBlocks).sortedWith(compareBy({ it.startDate }, { it.startSlot }))
        val trackIdsByBlock = pinnedTrackIdsByBlock + newBlockTracks.groupBy({ it.block.id }, { it.setlistTrackId })
        return ScheduleBoardResponse.of(board, blocks, trackIdsByBlock)
    }

    private fun deleteStaleBlocks(boardId: UUID) {
        val stale = scheduleBlockRepository.findAllByBoardIdAndPinned(boardId, false)
        if (stale.isEmpty()) return
        scheduleBlockTrackRepository.deleteAllByBlockIdIn(stale.map { it.id })
        scheduleBlockTrackRepository.flush()
        scheduleBlockRepository.deleteAllByBoardIdAndPinned(boardId, false)
        scheduleBlockRepository.flush()
    }

    private fun placeRound(
        pieces: List<Piece>,
        roundFrom: LocalDate,
        roundTo: LocalDate,
        effFrom: Int,
        effTo: Int,
        request: ScheduleAutoPlaceRequest,
        index: MemberAvailabilityIndex,
        occupancy: Occupancy,
        board: ScheduleBoard,
        newBlocks: MutableList<ScheduleBlock>,
        newBlockTracks: MutableList<ScheduleBlockTrack>,
    ) {
        val dates = generateSequence(roundFrom) { it.plusDays(1) }.takeWhile { !it.isAfter(roundTo) }.toList()
        val preferredDates = dates.filter { it.dayOfWeek in request.dayPreference }

        val candidates =
            pieces
                .mapNotNull { piece ->
                    val preferred = candidateStarts(piece, preferredDates, effFrom, effTo, index, occupancy)
                    if (preferred.isNotEmpty()) {
                        RoundCandidate(piece, preferred, fellBack = false)
                    } else {
                        val any = candidateStarts(piece, dates, effFrom, effTo, index, occupancy)
                        if (any.isNotEmpty()) RoundCandidate(piece, any, fellBack = true) else null
                    }
                }.sortedWith(compareByDescending<RoundCandidate> { it.fellBack }.thenBy { it.total })

        candidates.forEach { candidate ->
            val piece = candidate.piece
            val orderedDates =
                candidate.startsByDate.keys.sortedWith(
                    compareBy({ dayPriority(it.dayOfWeek, request.dayPreference) }, { it }),
                )
            for (date in orderedDates) {
                // 앞선 조각의 배치로 점유가 바뀌었으므로 재검증한다
                val valid =
                    candidate.startsByDate
                        .getValue(date)
                        .filter { occupancy.canPlace(piece.members, date, it, it + piece.lengthSlots) }
                if (valid.isEmpty()) continue
                val start = valid.minWithOrNull(compareBy({ damage(candidate, date, it, candidates) }, { it }))!!
                occupancy.place(piece.members, date, start, start + piece.lengthSlots)
                piece.trackIds.forEachIndexed { k, trackId ->
                    val block =
                        ScheduleBlock.create(
                            board = board,
                            slot =
                                Slot.ofDayRange(
                                    date,
                                    start + k * request.jamDurationSlots,
                                    start + (k + 1) * request.jamDurationSlots,
                                ),
                            placementOrigin = PlacementOrigin.AUTO,
                        )
                    newBlocks += block
                    newBlockTracks += ScheduleBlockTrack.create(block = block, setlistTrackId = trackId, ordinal = 0)
                }
                break
            }
        }
    }

    private fun candidateStarts(
        piece: Piece,
        dates: List<LocalDate>,
        effFrom: Int,
        effTo: Int,
        index: MemberAvailabilityIndex,
        occupancy: Occupancy,
    ): Map<LocalDate, List<Int>> {
        val lastStart = effTo - piece.lengthSlots
        if (lastStart < effFrom) return emptyMap()
        return dates
            .associateWith { date ->
                (effFrom..lastStart).filter { start ->
                    index.allAvailableThrough(piece.members, date, start, start + piece.lengthSlots) &&
                        occupancy.canPlace(piece.members, date, start, start + piece.lengthSlots)
                }
            }.filterValues { it.isNotEmpty() }
    }

    /** 이 자리를 쓰면 멤버가 겹치는 다른 조각의 후보가 몇 개 죽는지. */
    private fun damage(
        self: RoundCandidate,
        date: LocalDate,
        start: Int,
        all: List<RoundCandidate>,
    ): Int {
        val end = start + self.piece.lengthSlots
        return all
            .filter { it !== self && it.piece.members.any(self.piece.members::contains) }
            .sumOf { other ->
                other.startsByDate[date].orEmpty().count { s -> s < end && s + other.piece.lengthSlots > start }
            }
    }

    private fun dayPriority(
        day: DayOfWeek,
        preference: List<DayOfWeek>,
    ): Int = preference.indexOf(day).let { if (it >= 0) it else preference.size + day.value }

    private fun rounds(
        from: LocalDate,
        to: LocalDate,
        interval: Frequency,
    ): List<Pair<LocalDate, LocalDate>> {
        if (interval == Frequency.ONCE) return listOf(from to to)
        val biweeklyAnchor = from.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val result = mutableListOf<Pair<LocalDate, LocalDate>>()
        var start = from
        while (!start.isAfter(to)) {
            val next =
                when (interval) {
                    Frequency.DAILY -> start.plusDays(1)
                    Frequency.WEEKLY -> start.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                    Frequency.BIWEEKLY -> {
                        val elapsed = ChronoUnit.DAYS.between(biweeklyAnchor, start)
                        biweeklyAnchor.plusDays((elapsed / 14 + 1) * 14)
                    }
                    Frequency.MONTHLY -> start.withDayOfMonth(1).plusMonths(1)
                    Frequency.ONCE -> error("unreachable")
                }
            result += start to minOf(next.minusDays(1), to)
            start = next
        }
        return result
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

    private data class Piece(
        val trackIds: List<UUID>,
        val members: Set<Long>,
        val lengthSlots: Int,
    )

    private class RoundCandidate(
        val piece: Piece,
        val startsByDate: Map<LocalDate, List<Int>>,
        val fellBack: Boolean,
    ) {
        val total: Int = startsByDate.values.sumOf { it.size }
    }

    /** 멤버별 점유 슬롯과 모임(연속 구간은 병합해 1회) 추적. */
    private class Occupancy(
        private val maxJamsPerDay: Int,
        private val maxGap: Int,
    ) {
        private val occupied = HashMap<Pair<Long, LocalDate>, BooleanArray>()
        private val jams = HashMap<Pair<Long, LocalDate>, MutableList<Pair<Int, Int>>>()

        fun occupy(
            slot: Slot,
            members: Set<Long>,
        ) {
            var date = slot.startDate
            while (!date.isAfter(slot.endDate)) {
                val from = if (date == slot.startDate) slot.startSlot else 0
                val until = if (date == slot.endDate) slot.endSlot else Slot.SLOTS_PER_DAY
                if (from < until) members.forEach { place(it, date, from, until) }
                date = date.plusDays(1)
            }
        }

        fun place(
            members: Set<Long>,
            date: LocalDate,
            start: Int,
            end: Int,
        ) = members.forEach { place(it, date, start, end) }

        fun canPlace(
            members: Set<Long>,
            date: LocalDate,
            start: Int,
            end: Int,
        ): Boolean =
            members.all { member ->
                val arr = occupied[member to date]
                if (arr != null && (start until end).any { arr[it] }) return@all false
                canAddJam(jams[member to date].orEmpty(), start, end)
            }

        private fun canAddJam(
            existing: List<Pair<Int, Int>>,
            start: Int,
            end: Int,
        ): Boolean {
            val prev = existing.filter { it.second <= start }.maxByOrNull { it.second }
            val next = existing.filter { it.first >= end }.minByOrNull { it.first }
            if (prev != null && start - prev.second > maxGap) return false
            if (next != null && next.first - end > maxGap) return false
            val merges = (if (prev?.second == start) 1 else 0) + (if (next?.first == end) 1 else 0)
            return existing.size + 1 - merges <= maxJamsPerDay
        }

        private fun place(
            member: Long,
            date: LocalDate,
            start: Int,
            end: Int,
        ) {
            val key = member to date
            val arr = occupied.getOrPut(key) { BooleanArray(Slot.SLOTS_PER_DAY) }
            for (s in start until end) arr[s] = true
            val list = jams.getOrPut(key) { mutableListOf() }
            var mergedStart = start
            var mergedEnd = end
            list.removeAll { (s, e) ->
                if (e >= mergedStart && s <= mergedEnd) {
                    mergedStart = minOf(mergedStart, s)
                    mergedEnd = maxOf(mergedEnd, e)
                    true
                } else {
                    false
                }
            }
            list += mergedStart to mergedEnd
        }
    }
}
