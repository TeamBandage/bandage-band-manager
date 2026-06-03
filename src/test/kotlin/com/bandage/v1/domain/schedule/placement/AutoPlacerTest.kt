package com.bandage.v1.domain.schedule.placement

import com.bandage.v1.domain.availability.repository.MemberAvailabilityRepository
import com.bandage.v1.domain.jam.repository.JamReservationRepository
import com.bandage.v1.domain.schedule.placement.strategy.BlockShape
import com.bandage.v1.domain.schedule.placement.strategy.CoverageGoal
import com.bandage.v1.domain.schedule.placement.strategy.DateGenerator
import com.bandage.v1.domain.schedule.placement.strategy.HardConstraint
import com.bandage.v1.domain.schedule.placement.strategy.SoftScorer
import com.bandage.v1.domain.schedule.placement.strategy.StrategyComposition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.time.LocalDate
import java.util.UUID

class AutoPlacerTest {
    private val sut =
        AutoPlacer(AvailabilityCalculator(mock(MemberAvailabilityRepository::class.java), mock(JamReservationRepository::class.java)))

    private val windowFrom = LocalDate.of(2026, 6, 1)
    private val windowTo = LocalDate.of(2026, 6, 7)

    private fun strategy(
        hard: List<HardConstraint> = listOf(HardConstraint.WorkingHours(18, 46), HardConstraint.RequireAllAvailable()),
    ): StrategyComposition =
        StrategyComposition(
            dateGenerator = DateGenerator.AllDays(),
            hardConstraints = hard,
            softScorers = listOf(SoftScorer.AvailabilityScorer(1.0), SoftScorer.EarlyDateScorer(0.5)),
            blockShape = BlockShape(durationSlots = 4),
            coverageGoal = CoverageGoal(1),
        )

    private fun context(
        items: List<PlaceableItem>,
        strategy: StrategyComposition = strategy(),
    ): PlacementContext =
        PlacementContext(
            windowFrom = windowFrom,
            windowTo = windowTo,
            items = items,
            strategy = strategy,
            availabilityContext = AvailabilityContext(emptyMap(), emptyMap()),
        )

    @Test
    fun `가용성 레코드가 없는 멤버 블록은 근무시간 내에 배치된다`() {
        val item = PlaceableItem(UUID.randomUUID(), setOf(1L, 2L), sessionsNeeded = 1)

        val proposal = sut.propose(context(listOf(item)))

        assertThat(proposal.placedCount).isEqualTo(1)
        val block = proposal.blocks.first()
        assertThat(block.startSlot).isGreaterThanOrEqualTo(18)
        assertThat(block.endSlot).isLessThanOrEqualTo(46)
        assertThat(block.availabilityRatio).isEqualTo(1.0)
        assertThat(block.placementReasons).isNotEmpty()
    }

    @Test
    fun `멤버를 공유하는 두 항목은 시간이 겹치지 않게 배치된다`() {
        val shared = 1L
        val a = PlaceableItem(UUID.randomUUID(), setOf(shared), 1)
        val b = PlaceableItem(UUID.randomUUID(), setOf(shared), 1)

        val proposal = sut.propose(context(listOf(a, b)))

        assertThat(proposal.placedCount).isEqualTo(2)
        val (first, second) = proposal.blocks
        val overlap = first.date == second.date && first.startSlot < second.endSlot && first.endSlot > second.startSlot
        assertThat(overlap).isFalse()
    }

    @Test
    fun `만족 불가능한 hard 제약이면 미배치되고 nearMiss 가 기록된다`() {
        // 근무시간 [0,2) 에 길이 4 블록은 불가능
        val impossible = strategy(hard = listOf(HardConstraint.WorkingHours(0, 2)))
        val item = PlaceableItem(UUID.randomUUID(), setOf(1L), 1)

        val proposal = sut.propose(context(listOf(item), impossible))

        assertThat(proposal.placedCount).isEqualTo(0)
        assertThat(proposal.unplaced).singleElement()
        assertThat(proposal.unplaced.first().nearMisses).isNotEmpty()
    }

    @Test
    fun `MaxBlocksPerDay 제약이 있으면 블록이 여러 날에 분산된다`() {
        val strat =
            strategy(
                hard =
                    listOf(
                        HardConstraint.WorkingHours(18, 46),
                        HardConstraint.RequireAllAvailable(),
                        HardConstraint.MaxBlocksPerDay(maxPerDay = 1),
                    ),
            )
        // 같은 멤버가 4회 연습 → maxPerDay=1 이면 최소 4일에 분산되어야 함
        val item = PlaceableItem(UUID.randomUUID(), setOf(1L), sessionsNeeded = 4)

        val proposal = sut.propose(context(listOf(item), strat))

        assertThat(proposal.placedCount).isEqualTo(4)
        val distinctDates = proposal.blocks.map { it.date }.toSet()
        assertThat(distinctDates).hasSize(4) // 하루 1개 제약 → 4개 서로 다른 날
    }

    @Test
    fun `coverageGoal sessionsNeeded 만큼 블록이 배치된다`() {
        val item = PlaceableItem(UUID.randomUUID(), setOf(1L), sessionsNeeded = 3)

        val proposal = sut.propose(context(listOf(item)))

        assertThat(proposal.placedCount).isEqualTo(3)
        // 같은 멤버이므로 모두 시간이 겹치지 않아야 함
        val slots = proposal.blocks.map { it.date to it.startSlot }
        assertThat(slots).doesNotHaveDuplicates()
    }
}
