package com.bandage.v1.domain.schedule.placement.strategy

import com.bandage.v1.domain.schedule.placement.PlacementCandidate
import com.bandage.v1.domain.schedule.placement.ScoringEnv
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * 통과한 후보의 선호도를 점수화한다(높을수록 좋음). 가중치(weight)로 전략 간 상대 중요도를 조절한다.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = SoftScorer.AvailabilityScorer::class, name = "AVAILABILITY"),
    JsonSubTypes.Type(value = SoftScorer.TimeOfDayScorer::class, name = "TIME_OF_DAY"),
    JsonSubTypes.Type(value = SoftScorer.EarlyDateScorer::class, name = "EARLY_DATE"),
    JsonSubTypes.Type(value = SoftScorer.CompactnessScorer::class, name = "COMPACTNESS"),
)
sealed interface SoftScorer {
    val weight: Double

    fun score(
        candidate: PlacementCandidate,
        env: ScoringEnv,
    ): Double

    /** 가용 멤버 비율이 높을수록 높은 점수. */
    data class AvailabilityScorer(
        override val weight: Double = 1.0,
    ) : SoftScorer {
        override fun score(
            candidate: PlacementCandidate,
            env: ScoringEnv,
        ): Double = candidate.feasibility.availabilityRatio * weight
    }

    /** 선호 시작 슬롯에 가까울수록 높은 점수. */
    data class TimeOfDayScorer(
        val preferredStartSlot: Int,
        override val weight: Double = 1.0,
    ) : SoftScorer {
        override fun score(
            candidate: PlacementCandidate,
            env: ScoringEnv,
        ): Double {
            val proximity = 1.0 - abs(candidate.startSlot - preferredStartSlot) / SLOTS_PER_DAY
            return proximity.coerceIn(0.0, 1.0) * weight
        }
    }

    /** window 시작에 가까운(이른) 날짜일수록 높은 점수. */
    data class EarlyDateScorer(
        override val weight: Double = 1.0,
    ) : SoftScorer {
        override fun score(
            candidate: PlacementCandidate,
            env: ScoringEnv,
        ): Double {
            val span = ChronoUnit.DAYS.between(env.windowFrom, env.windowTo).coerceAtLeast(1)
            val offset = ChronoUnit.DAYS.between(env.windowFrom, candidate.date).coerceIn(0, span)
            return (1.0 - offset.toDouble() / span) * weight
        }
    }

    /** 같은 날 기존 pending 블록과 시간상 인접할수록 높은 점수(빈틈 최소화). */
    data class CompactnessScorer(
        override val weight: Double = 1.0,
    ) : SoftScorer {
        override fun score(
            candidate: PlacementCandidate,
            env: ScoringEnv,
        ): Double {
            val sameDay = env.pendingBlocks.filter { it.date == candidate.date }
            if (sameDay.isEmpty()) return 0.0
            val adjacent =
                sameDay.any { it.endSlot == candidate.startSlot || candidate.endSlot == it.startSlot }
            return if (adjacent) weight else 0.0
        }
    }

    companion object {
        private const val SLOTS_PER_DAY: Double = 48.0
    }
}
