package com.bandage.bandmanager.domain.schedule.placement.strategy

import com.bandage.bandmanager.domain.schedule.placement.PlacementCandidate
import com.bandage.bandmanager.domain.schedule.placement.ScoringEnv

/**
 * 배치할 블록의 형태(길이).
 */
data class BlockShape(
    val durationSlots: Int = 4,
)

/**
 * 커버리지 목표: 트랙당 몇 회 연습을 배치할지.
 */
data class CoverageGoal(
    val sessionsPerTrack: Int = 1,
)

/**
 * 자동 배치 전략 조합. DateGenerator(1) + HardConstraint(N) + SoftScorer(N) + BlockShape + CoverageGoal.
 * Jackson 다형성으로 직렬화 가능하여 보드별 전략을 저장/전달할 수 있다.
 */
data class StrategyComposition(
    val dateGenerator: DateGenerator,
    val hardConstraints: List<HardConstraint>,
    val softScorers: List<SoftScorer>,
    val blockShape: BlockShape = BlockShape(),
    val coverageGoal: CoverageGoal = CoverageGoal(),
) {
    fun passesHardConstraints(
        candidate: PlacementCandidate,
        env: ScoringEnv,
    ): Boolean = hardConstraints.all { it.isSatisfied(candidate, env) }

    fun totalScore(
        candidate: PlacementCandidate,
        env: ScoringEnv,
    ): Double = softScorers.sumOf { it.score(candidate, env) }
}

/**
 * 미리 정의된 전략 프리셋. 사용자가 세부 전략을 구성하지 않아도 합리적 기본값을 제공한다.
 *
 * 슬롯 기준: 30분 단위, slot 18 = 09:00, slot 46 = 23:00.
 */
enum class StrategyPreset {
    /** 균형: 평일/주말 모두, 과반 가용, 가용성·빠른날짜·인접성 균형 */
    BALANCED {
        override fun composition(): StrategyComposition =
            StrategyComposition(
                dateGenerator = DateGenerator.AllDays(),
                hardConstraints =
                    listOf(
                        HardConstraint.WorkingHours(startSlot = 18, endSlot = 46),
                        HardConstraint.MinAvailabilityRatio(minRatio = 0.5),
                        HardConstraint.MaxBlocksPerDay(maxPerDay = 2),
                    ),
                softScorers =
                    listOf(
                        SoftScorer.AvailabilityScorer(weight = 1.0),
                        SoftScorer.EarlyDateScorer(weight = 0.5),
                        SoftScorer.CompactnessScorer(weight = 0.3),
                    ),
                blockShape = BlockShape(durationSlots = 4),
                coverageGoal = CoverageGoal(sessionsPerTrack = 1),
            )
    },

    /** 집중: 매일, 전원 가용, 가용성 최우선 + 저녁 시간 선호 */
    INTENSIVE {
        override fun composition(): StrategyComposition =
            StrategyComposition(
                dateGenerator = DateGenerator.AllDays(),
                hardConstraints =
                    listOf(
                        HardConstraint.WorkingHours(startSlot = 18, endSlot = 46),
                        HardConstraint.RequireAllAvailable(),
                    ),
                softScorers =
                    listOf(
                        SoftScorer.AvailabilityScorer(weight = 1.0),
                        SoftScorer.TimeOfDayScorer(preferredStartSlot = 38, weight = 0.6),
                        SoftScorer.CompactnessScorer(weight = 0.4),
                    ),
                blockShape = BlockShape(durationSlots = 4),
                coverageGoal = CoverageGoal(sessionsPerTrack = 2),
            )
    },

    /** 여유: 주말만, 심야 제외, 과반 가용 + 빠른 날짜 선호 */
    RELAXED {
        override fun composition(): StrategyComposition =
            StrategyComposition(
                dateGenerator = DateGenerator.SpecificWeekdays(setOf(java.time.DayOfWeek.SATURDAY, java.time.DayOfWeek.SUNDAY)),
                hardConstraints =
                    listOf(
                        HardConstraint.WorkingHours(startSlot = 20, endSlot = 44),
                        HardConstraint.NoLateNight(boundarySlot = 44),
                        HardConstraint.MinAvailabilityRatio(minRatio = 0.5),
                        HardConstraint.MaxBlocksPerDay(maxPerDay = 2),
                    ),
                softScorers =
                    listOf(
                        SoftScorer.AvailabilityScorer(weight = 1.0),
                        SoftScorer.EarlyDateScorer(weight = 0.8),
                    ),
                blockShape = BlockShape(durationSlots = 4),
                coverageGoal = CoverageGoal(sessionsPerTrack = 1),
            )
    }, ;

    abstract fun composition(): StrategyComposition
}
