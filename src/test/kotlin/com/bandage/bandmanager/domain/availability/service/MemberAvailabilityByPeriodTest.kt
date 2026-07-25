package com.bandage.bandmanager.domain.availability.service

import com.bandage.bandmanager.domain.availability.model.AvailabilityException
import com.bandage.bandmanager.domain.availability.model.AvailabilityKind
import com.bandage.bandmanager.domain.availability.model.MemberAvailability
import com.bandage.bandmanager.domain.availability.model.WeeklyRule
import com.bandage.bandmanager.domain.availability.repository.MemberAvailabilityRepository
import com.bandage.bandmanager.global.error.exception.BusinessException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDate

class MemberAvailabilityByPeriodTest {
    private val repository = mock(MemberAvailabilityRepository::class.java)
    private val sut = MemberAvailabilityService(repository)

    private val mon = LocalDate.of(2026, 6, 1)

    private fun availability(
        rules: List<WeeklyRule> = emptyList(),
        exceptions: List<AvailabilityException> = emptyList(),
    ): MemberAvailability =
        MemberAvailability.create(1L).apply {
            updateWeeklyRules(rules)
            updateExceptions(exceptions)
        }

    @Test
    fun `조회 구간과 겹치는 규칙만 반환한다`() {
        val inRange = WeeklyRule(mon.dayOfWeek, 20, 44, effectiveFrom = mon, effectiveTo = mon.plusDays(10))
        // 조회 구간(mon ~ mon+7) 이후에만 유효 → 제외
        val outOfRange = WeeklyRule(mon.dayOfWeek, 20, 44, effectiveFrom = mon.plusDays(30), effectiveTo = mon.plusDays(40))
        `when`(repository.findByMemberId(1L)).thenReturn(availability(rules = listOf(inRange, outOfRange)))

        val result = sut.getMyAvailabilityByPeriod(1L, mon, mon.plusDays(7))

        assertThat(result.weeklyRules).hasSize(1)
        assertThat(result.weeklyRules.first().effectiveFrom).isEqualTo(mon)
    }

    @Test
    fun `무기한(effectiveTo == null) 규칙은 구간 시작 이전부터여도 포함된다`() {
        val openEnded = WeeklyRule(mon.dayOfWeek, 20, 44, effectiveFrom = mon.minusDays(100), effectiveTo = null)
        `when`(repository.findByMemberId(1L)).thenReturn(availability(rules = listOf(openEnded)))

        val result = sut.getMyAvailabilityByPeriod(1L, mon, mon.plusDays(7))

        assertThat(result.weeklyRules).hasSize(1)
    }

    @Test
    fun `조회 구간 안의 예외만 반환한다`() {
        val inRange = AvailabilityException(mon.plusDays(2), AvailabilityKind.BLOCKED)
        val outOfRange = AvailabilityException(mon.plusDays(20), AvailabilityKind.BLOCKED)
        `when`(repository.findByMemberId(1L)).thenReturn(availability(exceptions = listOf(inRange, outOfRange)))

        val result = sut.getMyAvailabilityByPeriod(1L, mon, mon.plusDays(7))

        assertThat(result.exceptions).hasSize(1)
        assertThat(result.exceptions.first().date).isEqualTo(mon.plusDays(2))
    }

    @Test
    fun `미등록 멤버는 빈 응답`() {
        `when`(repository.findByMemberId(1L)).thenReturn(null)

        val result = sut.getMyAvailabilityByPeriod(1L, mon, mon.plusDays(7))

        assertThat(result.memberId).isEqualTo(1L)
        assertThat(result.weeklyRules).isEmpty()
        assertThat(result.exceptions).isEmpty()
    }

    @Test
    fun `from 이 to 보다 늦으면 예외`() {
        assertThatThrownBy { sut.getMyAvailabilityByPeriod(1L, mon, mon.minusDays(1)) }
            .isInstanceOf(BusinessException::class.java)
    }

    @Test
    fun `366일을 초과하면 예외`() {
        assertThatThrownBy { sut.getMyAvailabilityByPeriod(1L, mon, mon.plusDays(400)) }
            .isInstanceOf(BusinessException::class.java)
    }
}
