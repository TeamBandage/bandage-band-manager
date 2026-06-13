package com.bandage.bandmanager.domain.schedule.service

import com.bandage.bandmanager.domain.availability.model.MemberAvailability
import com.bandage.bandmanager.domain.availability.repository.MemberAvailabilityRepository
import com.bandage.bandmanager.domain.schedule.model.MemberSchedule
import com.bandage.bandmanager.domain.schedule.repository.MemberScheduleRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDate

class MemberScheduleMigrationServiceTest {
    private val memberScheduleRepository = mock(MemberScheduleRepository::class.java)
    private val memberAvailabilityRepository = mock(MemberAvailabilityRepository::class.java)
    private val sut = MemberScheduleMigrationService(memberScheduleRepository, memberAvailabilityRepository)

    private fun schedule(
        meetingId: java.util.UUID,
        userId: Long,
        available: Set<LocalDate>,
        unavailable: Set<LocalDate>,
    ): MemberSchedule =
        MemberSchedule.create(meetingId = meetingId, userId = userId).apply {
            updateAvailability(availableDates = available, unavailableDates = unavailable, blocks = null)
        }

    @Test
    fun `available_unavailable 날짜가 전일 예외로 전환되고 BLOCKED 가 우선한다`() {
        val d1 = LocalDate.of(2026, 6, 1)
        val d2 = LocalDate.of(2026, 6, 2)
        val conflict = LocalDate.of(2026, 6, 3)
        `when`(memberScheduleRepository.findAllByUserId(1L)).thenReturn(
            listOf(
                schedule(java.util.UUID.randomUUID(), 1L, available = setOf(d1, conflict), unavailable = setOf(d2)),
                schedule(java.util.UUID.randomUUID(), 1L, available = emptySet(), unavailable = setOf(conflict)),
            ),
        )
        `when`(memberAvailabilityRepository.findByMemberId(1L)).thenReturn(null)
        `when`(memberAvailabilityRepository.save(any(MemberAvailability::class.java)))
            .thenAnswer { it.getArgument(0) }

        val migrated = sut.migrateMember(1L)

        // d1=AVAILABLE, d2=BLOCKED, conflict=BLOCKED (available 에서 제외) → 총 3건
        assertThat(migrated).isEqualTo(3)
    }

    @Test
    fun `스케줄이 없으면 0건 전환`() {
        `when`(memberScheduleRepository.findAllByUserId(99L)).thenReturn(emptyList())
        assertThat(sut.migrateMember(99L)).isEqualTo(0)
    }
}
