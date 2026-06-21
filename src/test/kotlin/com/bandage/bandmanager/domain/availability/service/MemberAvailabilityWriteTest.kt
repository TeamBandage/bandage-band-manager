package com.bandage.bandmanager.domain.availability.service

import com.bandage.bandmanager.domain.availability.dto.req.MemberAvailabilityRequest
import com.bandage.bandmanager.domain.availability.dto.req.MemberAvailabilityRequest.AvailabilityExceptionRequest
import com.bandage.bandmanager.domain.availability.dto.req.MemberAvailabilityRequest.WeeklyRuleRequest
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
import java.time.DayOfWeek
import java.time.LocalDate

class MemberAvailabilityWriteTest {
    private val repository = mock(MemberAvailabilityRepository::class.java)
    private val sut = MemberAvailabilityService(repository)

    private val day = DayOfWeek.MONDAY
    private val from = LocalDate.of(2026, 6, 13)
    private val to = LocalDate.of(2026, 7, 1)

    /** 기존(seeded) 패턴: 월 18~22h (slot 36~44). */
    private fun seeded(
        f: LocalDate,
        t: LocalDate?,
    ) = WeeklyRule(day, startSlot = 36, endSlot = 44, effectiveFrom = f, effectiveTo = t)

    private fun seed(
        rules: List<WeeklyRule> = emptyList(),
        exceptions: List<AvailabilityException> = emptyList(),
    ): MemberAvailability {
        val av =
            MemberAvailability.create(1L).apply {
                updateWeeklyRules(rules)
                updateExceptions(exceptions)
            }
        `when`(repository.findByMemberId(1L)).thenReturn(av)
        `when`(repository.save(av)).thenReturn(av)
        return av
    }

    /** 기본 신규 규칙은 기존과 다른 슬롯(20~24)이라 clip 조각과 병합되지 않아 조각이 관찰된다. */
    private fun request(
        startSlot: Int = 20,
        endSlot: Int = 24,
        exceptions: List<AvailabilityExceptionRequest> = emptyList(),
    ) = MemberAvailabilityRequest(
        effectiveFrom = from,
        effectiveTo = to,
        weeklyRules = listOf(WeeklyRuleRequest(day, startSlot, endSlot)),
        exceptions = exceptions,
    )

    /** (startSlot, effectiveFrom, effectiveTo) — startSlot 36/20 으로 패턴 구분. */
    private fun rows(av: MemberAvailability) = av.weeklyRules.map { tuple(it.startSlot, it.effectiveFrom, it.effectiveTo) }

    // ---------- clip: 다른 패턴 신규 → 잘린 조각이 그대로 남는다 ----------

    @Test
    fun `구간에 머리만 걸친 규칙은 잘리고 새 규칙이 들어간다`() {
        val av = seed(rules = listOf(seeded(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 15))))

        sut.updateMyAvailability(1L, request())

        assertThat(rows(av)).containsExactlyInAnyOrder(
            tuple(36, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 12)),
            tuple(20, from, to),
        )
    }

    @Test
    fun `구간이 규칙 가운데를 관통하면 머리+꼬리로 split 된다`() {
        val av = seed(rules = listOf(seeded(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 31))))

        sut.updateMyAvailability(1L, request())

        assertThat(rows(av)).containsExactlyInAnyOrder(
            tuple(36, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 12)),
            tuple(36, LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 31)),
            tuple(20, from, to),
        )
    }

    @Test
    fun `구간에 완전히 포함된 규칙은 제거되고 새 규칙으로 대체된다`() {
        val av = seed(rules = listOf(seeded(LocalDate.of(2026, 6, 14), LocalDate.of(2026, 6, 20))))

        sut.updateMyAvailability(1L, request())

        assertThat(rows(av)).containsExactly(tuple(20, from, to))
    }

    @Test
    fun `구간과 겹치지 않는 규칙은 그대로 보존된다`() {
        val av = seed(rules = listOf(seeded(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))))

        sut.updateMyAvailability(1L, request())

        assertThat(rows(av)).containsExactlyInAnyOrder(
            tuple(36, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)),
            tuple(20, from, to),
        )
    }

    @Test
    fun `무기한 규칙은 꼬리가 무기한으로 유지된다`() {
        val av = seed(rules = listOf(seeded(LocalDate.of(2026, 6, 1), null)))

        sut.updateMyAvailability(1L, request())

        assertThat(rows(av)).containsExactlyInAnyOrder(
            tuple(36, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 12)),
            tuple(36, LocalDate.of(2026, 7, 2), null),
            tuple(20, from, to),
        )
    }

    // ---------- coalesce: 같은 패턴 신규 → 맞닿은 조각이 다시 병합된다 ----------

    @Test
    fun `같은 패턴을 겹치는 구간에 다시 칠하면 하나로 병합된다`() {
        val av = seed(rules = listOf(seeded(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 15))))

        sut.updateMyAvailability(1L, request(startSlot = 36, endSlot = 44))

        // 머리(6/1~6/12) + 신규(6/13~7/1) 동일 패턴 → (6/1~7/1) 한 줄
        assertThat(rows(av)).containsExactly(tuple(36, LocalDate.of(2026, 6, 1), to))
    }

    @Test
    fun `같은 패턴이면 split 된 무기한 규칙이 다시 무기한 한 줄로 자가치유된다`() {
        val av = seed(rules = listOf(seeded(LocalDate.of(2026, 6, 1), null)))

        sut.updateMyAvailability(1L, request(startSlot = 36, endSlot = 44))

        // 머리 + 신규 + 무기한 꼬리 모두 동일 패턴·연속 → (6/1~무기한) 한 줄로 복원(저장 멱등)
        assertThat(rows(av)).containsExactly(tuple(36, LocalDate.of(2026, 6, 1), null))
    }

    @Test
    fun `날짜가 맞닿아도 패턴이 다르면 병합하지 않는다`() {
        val av = seed(rules = listOf(seeded(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 12))))

        sut.updateMyAvailability(1L, request(startSlot = 20, endSlot = 24))

        assertThat(rows(av)).containsExactlyInAnyOrder(
            tuple(36, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 12)),
            tuple(20, from, to),
        )
    }

    // ---------- 예외 / 검증 ----------

    @Test
    fun `구간 안 예외는 교체되고 구간 밖 예외는 보존된다`() {
        val outside = AvailabilityException(LocalDate.of(2026, 6, 1), AvailabilityKind.BLOCKED)
        val inside = AvailabilityException(LocalDate.of(2026, 6, 20), AvailabilityKind.BLOCKED)
        val av = seed(exceptions = listOf(outside, inside))

        sut.updateMyAvailability(
            1L,
            request(exceptions = listOf(AvailabilityExceptionRequest(LocalDate.of(2026, 6, 14), AvailabilityKind.BLOCKED))),
        )

        assertThat(av.exceptions.map { it.date })
            .containsExactlyInAnyOrder(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 14))
    }

    @Test
    fun `구간 밖 날짜의 예외를 제출하면 AVAILABILITY_INVALID`() {
        seed()

        assertThatThrownBy {
            sut.updateMyAvailability(
                1L,
                request(exceptions = listOf(AvailabilityExceptionRequest(LocalDate.of(2026, 8, 1), AvailabilityKind.BLOCKED))),
            )
        }.isInstanceOf(BusinessException::class.java)
    }

    @Test
    fun `effectiveFrom 이 effectiveTo 보다 늦으면 AVAILABILITY_INVALID`() {
        assertThatThrownBy {
            sut.updateMyAvailability(
                1L,
                MemberAvailabilityRequest(effectiveFrom = to, effectiveTo = from),
            )
        }.isInstanceOf(BusinessException::class.java)
    }
}
