package com.bandage.bandmanager.domain.availability.service

import com.bandage.bandmanager.domain.availability.model.AvailabilityException
import com.bandage.bandmanager.domain.availability.model.AvailabilityKind
import com.bandage.bandmanager.domain.availability.model.MemberAvailability
import com.bandage.bandmanager.domain.availability.model.WeeklyRule
import com.bandage.bandmanager.domain.availability.repository.MemberAvailabilityRepository
import com.bandage.bandmanager.global.error.exception.BusinessException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDate

class MemberAvailabilitySlotsTest {
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
    fun `주간 규칙을 날짜별 가용 슬롯으로 전개한다`() {
        // mon 요일, 10:00~22:00(slot 20~44)
        val rule = WeeklyRule(mon.dayOfWeek, startSlot = 20, endSlot = 44, effectiveFrom = mon)
        `when`(repository.findByMemberId(1L)).thenReturn(availability(rules = listOf(rule)))

        val slots = sut.getMySlots(1L, mon, mon.plusDays(7))

        // mon 과 mon+7 두 번만 해당 요일 → 슬롯 2개
        assertThat(slots.map { it.date }).containsExactly(mon, mon.plusDays(7))
        assertThat(slots).allSatisfy {
            assertThat(it.startSlot).isEqualTo(20)
            assertThat(it.endSlot).isEqualTo(44)
        }
    }

    @Test
    fun `BLOCKED 예외 구간은 가용 슬롯에서 도려내져 두 구간으로 쪼개진다`() {
        val rule = WeeklyRule(mon.dayOfWeek, 20, 44, effectiveFrom = mon)
        val blocked = AvailabilityException(mon, AvailabilityKind.BLOCKED, startSlot = 30, endSlot = 34)
        `when`(repository.findByMemberId(1L)).thenReturn(availability(rules = listOf(rule), exceptions = listOf(blocked)))

        val slots = sut.getMySlots(1L, mon, mon)

        assertThat(slots)
            .extracting("startSlot", "endSlot")
            .containsExactly(tuple(20, 30), tuple(34, 44))
    }

    @Test
    fun `미등록 멤버는 빈 리스트`() {
        `when`(repository.findByMemberId(1L)).thenReturn(null)

        assertThat(sut.getMySlots(1L, mon, mon.plusDays(7))).isEmpty()
    }

    @Test
    fun `from 이 to 보다 늦으면 예외`() {
        assertThatThrownBy { sut.getMySlots(1L, mon, mon.minusDays(1)) }
            .isInstanceOf(BusinessException::class.java)
    }

    @Test
    fun `366일을 초과하면 예외`() {
        assertThatThrownBy { sut.getMySlots(1L, mon, mon.plusDays(400)) }
            .isInstanceOf(BusinessException::class.java)
    }
}
