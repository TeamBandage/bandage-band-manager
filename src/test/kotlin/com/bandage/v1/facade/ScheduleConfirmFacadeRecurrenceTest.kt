package com.bandage.v1.facade

import com.bandage.v1.domain.jam.repository.JamRepository
import com.bandage.v1.domain.jam.service.JamReservationSyncService
import com.bandage.v1.domain.jam.service.SetlistTrackToJamConverter
import com.bandage.v1.domain.schedule.model.RecurrenceFreq
import com.bandage.v1.domain.schedule.model.RecurrenceRule
import com.bandage.v1.domain.schedule.model.ScheduleBlock
import com.bandage.v1.domain.schedule.model.ScheduleBoard
import com.bandage.v1.domain.schedule.repository.ScheduleBlockJamRepository
import com.bandage.v1.domain.schedule.repository.ScheduleBlockRepository
import com.bandage.v1.domain.schedule.repository.ScheduleBlockTrackRepository
import com.bandage.v1.domain.schedule.repository.ScheduleBoardRepository
import com.bandage.v1.domain.schedule.service.ScheduleAuthService
import com.bandage.v1.domain.setlist.repository.SetlistTrackParticipantRepository
import com.bandage.v1.domain.setlist.repository.SetlistTrackRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.time.LocalDate

class ScheduleConfirmFacadeRecurrenceTest {
    private val sut =
        ScheduleConfirmFacade(
            mock(ScheduleBoardRepository::class.java),
            mock(ScheduleBlockRepository::class.java),
            mock(ScheduleBlockTrackRepository::class.java),
            mock(ScheduleBlockJamRepository::class.java),
            mock(SetlistTrackRepository::class.java),
            mock(SetlistTrackParticipantRepository::class.java),
            mock(SetlistTrackToJamConverter::class.java),
            mock(JamRepository::class.java),
            mock(JamReservationSyncService::class.java),
            mock(ScheduleAuthService::class.java),
        )

    private val anchor = LocalDate.of(2026, 6, 1)

    private fun block(rule: RecurrenceRule): ScheduleBlock {
        val board = ScheduleBoard.create(performanceId = java.util.UUID.randomUUID(), name = "board")
        return ScheduleBlock.create(
            board = board,
            date = anchor,
            startSlot = 36,
            durationSlots = 4,
            recurrenceRule = rule,
        )
    }

    @Test
    fun `단발성(NONE) 블록은 anchor 1건만 전개된다`() {
        val result = sut.expandRecurrence(block(RecurrenceRule.none()), boardWindowTo = LocalDate.of(2026, 12, 31))
        assertThat(result).containsExactly(anchor)
    }

    @Test
    fun `WEEKLY + until 은 anchor 부터 until 까지 주 단위로 전개된다`() {
        val rule = RecurrenceRule(freq = RecurrenceFreq.WEEKLY, interval = 1, until = LocalDate.of(2026, 6, 29))
        val result = sut.expandRecurrence(block(rule), boardWindowTo = null)
        assertThat(result).containsExactly(
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 8),
            LocalDate.of(2026, 6, 15),
            LocalDate.of(2026, 6, 22),
            LocalDate.of(2026, 6, 29),
        )
    }

    @Test
    fun `DAILY + count 는 count 만큼만 전개된다`() {
        val rule = RecurrenceRule(freq = RecurrenceFreq.DAILY, interval = 1, count = 3)
        val result = sut.expandRecurrence(block(rule), boardWindowTo = null)
        assertThat(result).containsExactly(
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 2),
            LocalDate.of(2026, 6, 3),
        )
    }

    @Test
    fun `BIWEEKLY 는 보드 window 끝까지 격주로 전개된다`() {
        val rule = RecurrenceRule(freq = RecurrenceFreq.BIWEEKLY, interval = 1)
        val result = sut.expandRecurrence(block(rule), boardWindowTo = LocalDate.of(2026, 6, 30))
        assertThat(result).containsExactly(
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 15),
            LocalDate.of(2026, 6, 29),
        )
    }

    @Test
    fun `경계(until-window-count)가 전혀 없으면 무한 전개 방지로 단발성 처리된다`() {
        val rule = RecurrenceRule(freq = RecurrenceFreq.WEEKLY, interval = 1)
        val result = sut.expandRecurrence(block(rule), boardWindowTo = null)
        assertThat(result).containsExactly(anchor)
    }
}
