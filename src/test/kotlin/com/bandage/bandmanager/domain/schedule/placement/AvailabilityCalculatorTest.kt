package com.bandage.bandmanager.domain.schedule.placement

import com.bandage.bandmanager.domain.availability.model.AvailabilityException
import com.bandage.bandmanager.domain.availability.model.AvailabilityKind
import com.bandage.bandmanager.domain.availability.model.MemberAvailability
import com.bandage.bandmanager.domain.availability.model.WeeklyRule
import com.bandage.bandmanager.domain.availability.repository.MemberAvailabilityRepository
import com.bandage.bandmanager.domain.jam.model.JamReservation
import com.bandage.bandmanager.domain.jam.repository.JamReservationRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.time.LocalDate
import java.util.UUID

class AvailabilityCalculatorTest {
    private val sut =
        AvailabilityCalculator(
            mock(MemberAvailabilityRepository::class.java),
            mock(JamReservationRepository::class.java),
        )

    private val date = LocalDate.of(2026, 6, 10)

    // 18:00~22:00 = slot 36..44
    private val reqStart = 36
    private val reqDuration = 4

    private fun availabilityWith(
        memberId: Long,
        rules: List<WeeklyRule> = emptyList(),
        exceptions: List<AvailabilityException> = emptyList(),
    ): MemberAvailability =
        MemberAvailability.create(memberId).apply {
            updateWeeklyRules(rules)
            updateExceptions(exceptions)
        }

    @Test
    fun `주간 규칙이 요청 구간을 덮으면 가용하다`() {
        val rule = WeeklyRule(date.dayOfWeek, startSlot = 34, endSlot = 46, effectiveFrom = LocalDate.of(2026, 1, 1))
        val context = AvailabilityContext(mapOf(1L to availabilityWith(1L, rules = listOf(rule))), emptyMap())

        val result = sut.evaluate(context, date, reqStart, reqDuration, listOf(1L))

        assertThat(result.feasible).isTrue()
        assertThat(result.availableMembers).containsExactly(1L)
        assertThat(result.availabilityRatio).isEqualTo(1.0)
    }

    @Test
    fun `BLOCKED 예외가 겹치면 UNAVAILABLE 충돌`() {
        val rule = WeeklyRule(date.dayOfWeek, 34, 46, LocalDate.of(2026, 1, 1))
        val blocked = AvailabilityException(date, AvailabilityKind.BLOCKED, startSlot = 38, endSlot = 40)
        val context =
            AvailabilityContext(
                mapOf(1L to availabilityWith(1L, rules = listOf(rule), exceptions = listOf(blocked))),
                emptyMap(),
            )

        val result = sut.evaluate(context, date, reqStart, reqDuration, listOf(1L))

        assertThat(result.feasible).isFalse()
        assertThat(result.conflicts).singleElement()
        assertThat(result.conflicts.first().reason).isEqualTo(ConflictReason.UNAVAILABLE)
    }

    @Test
    fun `JamReservation 과 시간이 겹치면 JAM_RESERVATION 충돌`() {
        val rule = WeeklyRule(date.dayOfWeek, 34, 46, LocalDate.of(2026, 1, 1))
        val reservation =
            JamReservation.create(
                memberId = 1L,
                jamId = UUID.randomUUID(),
                startAt = date.atTime(18, 30),
                endAt = date.atTime(20, 0),
            )
        val context =
            AvailabilityContext(
                mapOf(1L to availabilityWith(1L, rules = listOf(rule))),
                mapOf(1L to listOf(reservation)),
            )

        val result = sut.evaluate(context, date, reqStart, reqDuration, listOf(1L))

        assertThat(result.conflicts.first().reason).isEqualTo(ConflictReason.JAM_RESERVATION)
    }

    @Test
    fun `같은 보드 pending 블록과 겹치면 PENDING_BLOCK 충돌`() {
        val rule = WeeklyRule(date.dayOfWeek, 34, 46, LocalDate.of(2026, 1, 1))
        val context = AvailabilityContext(mapOf(1L to availabilityWith(1L, rules = listOf(rule))), emptyMap())
        val pending = PendingBlock(date, startSlot = 38, durationSlots = 2, memberIds = setOf(1L))

        val result = sut.evaluate(context, date, reqStart, reqDuration, listOf(1L), listOf(pending))

        assertThat(result.conflicts.first().reason).isEqualTo(ConflictReason.PENDING_BLOCK)
    }

    @Test
    fun `가용성 레코드가 없는 멤버는 낙관적으로 가용 처리된다`() {
        val context = AvailabilityContext(emptyMap(), emptyMap())

        val result = sut.evaluate(context, date, reqStart, reqDuration, listOf(99L))

        assertThat(result.feasible).isTrue()
        assertThat(result.availableMembers).containsExactly(99L)
    }

    @Test
    fun `일부만 가용하면 비율과 충돌이 함께 보고된다`() {
        val rule = WeeklyRule(date.dayOfWeek, 34, 46, LocalDate.of(2026, 1, 1))
        val blocked = AvailabilityException(date, AvailabilityKind.BLOCKED, null, null) // 전일 차단
        val context =
            AvailabilityContext(
                mapOf(
                    1L to availabilityWith(1L, rules = listOf(rule)),
                    2L to availabilityWith(2L, rules = listOf(rule), exceptions = listOf(blocked)),
                ),
                emptyMap(),
            )

        val result = sut.evaluate(context, date, reqStart, reqDuration, listOf(1L, 2L))

        assertThat(result.availableMembers).containsExactly(1L)
        assertThat(result.availabilityRatio).isEqualTo(0.5)
        assertThat(result.conflicts).singleElement()
    }
}
