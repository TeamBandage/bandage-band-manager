package com.bandage.v1.domain.schedule.placement.strategy

import com.bandage.v1.domain.schedule.placement.PlacementCandidate
import com.bandage.v1.domain.schedule.placement.ScoringEnv
import com.bandage.v1.domain.schedule.placement.SlotFeasibility
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate

class StrategyCompositionTest {
    private val mapper = jacksonObjectMapper()

    private fun candidate(
        date: LocalDate,
        startSlot: Int,
        durationSlots: Int,
        ratio: Double,
    ): PlacementCandidate {
        val total = 4
        val availableCount = (total * ratio).toInt()
        return PlacementCandidate(
            date = date,
            startSlot = startSlot,
            durationSlots = durationSlots,
            feasibility =
                SlotFeasibility(
                    date = date,
                    startSlot = startSlot,
                    durationSlots = durationSlots,
                    totalMembers = total,
                    availableMembers = (1..availableCount).map { it.toLong() },
                    conflicts = emptyList(),
                ),
        )
    }

    @Test
    fun `StrategyComposition 은 Jackson 다형성으로 라운드트립된다`() {
        val original = StrategyPreset.BALANCED.composition()

        val json = mapper.writeValueAsString(original)
        val restored = mapper.readValue(json, StrategyComposition::class.java)

        assertThat(restored).isEqualTo(original)
        assertThat(json).contains("ALL_DAYS", "MIN_AVAILABILITY_RATIO", "AVAILABILITY")
    }

    @Test
    fun `모든 프리셋이 직렬화 라운드트립을 통과한다`() {
        StrategyPreset.entries.forEach { preset ->
            val comp = preset.composition()
            val restored = mapper.readValue(mapper.writeValueAsString(comp), StrategyComposition::class.java)
            assertThat(restored).isEqualTo(comp)
        }
    }

    @Test
    fun `SpecificWeekdays 는 지정 요일만 생성한다`() {
        val gen = DateGenerator.SpecificWeekdays(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
        val dates = gen.generate(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 14))
        assertThat(dates).allMatch { it.dayOfWeek == DayOfWeek.SATURDAY || it.dayOfWeek == DayOfWeek.SUNDAY }
        assertThat(dates).contains(LocalDate.of(2026, 6, 6), LocalDate.of(2026, 6, 7))
    }

    @Test
    fun `WorkingHours 와 MinAvailabilityRatio HardConstraint 가 동작한다`() {
        val date = LocalDate.of(2026, 6, 10)
        val within = candidate(date, startSlot = 20, durationSlots = 4, ratio = 1.0)
        val outside = candidate(date, startSlot = 4, durationSlots = 4, ratio = 1.0)
        val lowRatio = candidate(date, startSlot = 20, durationSlots = 4, ratio = 0.25)

        assertThat(HardConstraint.WorkingHours(18, 46).isSatisfied(within)).isTrue()
        assertThat(HardConstraint.WorkingHours(18, 46).isSatisfied(outside)).isFalse()
        assertThat(HardConstraint.MinAvailabilityRatio(0.5).isSatisfied(lowRatio)).isFalse()
    }

    @Test
    fun `totalScore 는 SoftScorer 가중합이다`() {
        val comp = StrategyPreset.BALANCED.composition()
        val date = LocalDate.of(2026, 6, 1)
        val env = ScoringEnv(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), emptyList())
        val c = candidate(date, startSlot = 20, durationSlots = 4, ratio = 1.0)

        // AvailabilityScorer(1.0)*1.0 + EarlyDateScorer(0.5)*1.0(window 시작일) + CompactnessScorer(0.3)*0(pending 없음)
        assertThat(comp.totalScore(c, env)).isEqualTo(1.5)
    }
}
