package com.bandage.bandmanager.domain.schedule.service

import com.bandage.bandmanager.domain.availability.model.AvailabilityException
import com.bandage.bandmanager.domain.availability.model.AvailabilityKind
import com.bandage.bandmanager.domain.availability.model.MemberAvailability
import com.bandage.bandmanager.domain.availability.model.WeeklyRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * 자동배치와 슬롯 가용 조회가 공유하는 인덱스 검증.
 *
 * isAvailableAt 을 매번 호출하는 대신 미리 펼쳐 두는 구조이므로,
 * 원본 판정과 결과가 어긋나지 않는지(주간 규칙/예외/미등록)를 확인한다.
 */
class MemberAvailabilityIndexTest {
    private val mon: LocalDate = LocalDate.of(2026, 6, 1)
    private val tue: LocalDate = mon.plusDays(1)

    private fun availability(
        memberId: Long,
        rules: List<WeeklyRule> = emptyList(),
        exceptions: List<AvailabilityException> = emptyList(),
    ): MemberAvailability =
        MemberAvailability.create(memberId).apply {
            updateWeeklyRules(rules)
            updateExceptions(exceptions)
        }

    /** 월요일 18:00~22:00(슬롯 36~44) 가용 */
    private fun mondayEvening(memberId: Long) =
        availability(
            memberId,
            rules = listOf(WeeklyRule(mon.dayOfWeek, startSlot = 36, endSlot = 44, effectiveFrom = mon)),
        )

    @Test
    fun `주간 규칙 범위 안의 슬롯만 가용으로 펼친다`() {
        val index = MemberAvailabilityIndex.build(listOf(1L), listOf(mondayEvening(1L)), mon, mon)

        assertThat(index.isAvailable(1L, mon, 36)).isTrue()
        assertThat(index.isAvailable(1L, mon, 43)).isTrue()
        // endSlot 44 는 반열린 구간이라 미포함
        assertThat(index.isAvailable(1L, mon, 44)).isFalse()
        assertThat(index.isAvailable(1L, mon, 35)).isFalse()
    }

    @Test
    fun `가용성 미등록 멤버는 항상 가용으로 본다`() {
        val index = MemberAvailabilityIndex.build(listOf(1L, 2L), listOf(mondayEvening(1L)), mon, mon)

        assertThat(index.isAvailable(2L, mon, 0)).isTrue()
        assertThat(index.isAvailable(2L, mon, 47)).isTrue()
    }

    @Test
    fun `BLOCKED 예외는 주간 규칙보다 우선한다`() {
        val blocked =
            availability(
                1L,
                rules = listOf(WeeklyRule(mon.dayOfWeek, startSlot = 36, endSlot = 44, effectiveFrom = mon)),
                exceptions = listOf(AvailabilityException(mon, AvailabilityKind.BLOCKED, 38, 40)),
            )
        val index = MemberAvailabilityIndex.build(listOf(1L), listOf(blocked), mon, mon)

        assertThat(index.isAvailable(1L, mon, 37)).isTrue()
        assertThat(index.isAvailable(1L, mon, 38)).isFalse()
        assertThat(index.isAvailable(1L, mon, 39)).isFalse()
        assertThat(index.isAvailable(1L, mon, 40)).isTrue()
    }

    @Test
    fun `슬롯별 가용 멤버 목록을 반환한다`() {
        val index =
            MemberAvailabilityIndex.build(
                listOf(1L, 2L, 3L),
                listOf(mondayEvening(1L), mondayEvening(2L)),
                mon,
                mon,
            )

        // 3L 은 미등록이라 항상 가용
        assertThat(index.availableMembersAt(mon, 36)).containsExactly(1L, 2L, 3L)
        assertThat(index.availableMembersAt(mon, 10)).containsExactly(3L)
    }

    @Test
    fun `구간 전체에 전원이 가용해야 통과한다`() {
        val index =
            MemberAvailabilityIndex.build(listOf(1L, 2L), listOf(mondayEvening(1L), mondayEvening(2L)), mon, mon)

        assertThat(index.allAvailableThrough(listOf(1L, 2L), mon, 36, 44)).isTrue()
        // 44 를 포함하면 규칙 범위를 벗어난다
        assertThat(index.allAvailableThrough(listOf(1L, 2L), mon, 36, 45)).isFalse()
        assertThat(index.allAvailableThrough(listOf(1L, 2L), mon, 35, 40)).isFalse()
    }

    @Test
    fun `한 명이라도 불가하면 구간 판정은 실패한다`() {
        val blocked =
            availability(
                2L,
                rules = listOf(WeeklyRule(mon.dayOfWeek, startSlot = 36, endSlot = 44, effectiveFrom = mon)),
                exceptions = listOf(AvailabilityException(mon, AvailabilityKind.BLOCKED, 38, 40)),
            )
        val index = MemberAvailabilityIndex.build(listOf(1L, 2L), listOf(mondayEvening(1L), blocked), mon, mon)

        assertThat(index.allAvailableThrough(listOf(1L), mon, 36, 44)).isTrue()
        assertThat(index.allAvailableThrough(listOf(1L, 2L), mon, 36, 44)).isFalse()
    }

    @Test
    fun `인덱스 범위 밖의 날짜는 불가로 본다`() {
        val index = MemberAvailabilityIndex.build(listOf(1L), listOf(mondayEvening(1L)), mon, mon)

        assertThat(index.isAvailable(1L, tue, 36)).isFalse()
        assertThat(index.availableMembersAt(tue, 36)).isEmpty()
        assertThat(index.allAvailableThrough(listOf(1L), tue, 36, 44)).isFalse()
    }

    @Test
    fun `여러 날짜를 펼친다`() {
        val everyDay =
            availability(
                1L,
                rules =
                    (0..6).map {
                        WeeklyRule(mon.plusDays(it.toLong()).dayOfWeek, startSlot = 36, endSlot = 44, effectiveFrom = mon)
                    },
            )
        val index = MemberAvailabilityIndex.build(listOf(1L), listOf(everyDay), mon, mon.plusDays(6))

        assertThat(index.isAvailable(1L, mon, 36)).isTrue()
        assertThat(index.isAvailable(1L, mon.plusDays(6), 36)).isTrue()
        assertThat(index.isAvailable(1L, mon.plusDays(7), 36)).isFalse()
    }

    @Test
    fun `대상 멤버가 비어 있으면 구간 판정은 통과한다`() {
        val index = MemberAvailabilityIndex.build(listOf(1L), listOf(mondayEvening(1L)), mon, mon)

        assertThat(index.allAvailableThrough(emptyList(), mon, 0, 48)).isTrue()
    }
}
