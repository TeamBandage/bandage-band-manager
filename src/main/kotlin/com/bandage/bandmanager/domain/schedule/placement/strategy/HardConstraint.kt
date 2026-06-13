package com.bandage.bandmanager.domain.schedule.placement.strategy

import com.bandage.bandmanager.domain.schedule.placement.PlacementCandidate
import com.bandage.bandmanager.domain.schedule.placement.ScoringEnv
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * 반드시 만족해야 하는 제약. 하나라도 위반하면 후보는 탈락한다.
 *
 * isSatisfied 는 단건 후보뿐 아니라 현재까지의 부분해([ScoringEnv.pendingBlocks]) 를 함께 받는다.
 * (예: 하루 배치 개수 상한처럼 이미 배치된 블록에 의존하는 제약을 표현하기 위함)
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = HardConstraint.WorkingHours::class, name = "WORKING_HOURS"),
    JsonSubTypes.Type(value = HardConstraint.NoLateNight::class, name = "NO_LATE_NIGHT"),
    JsonSubTypes.Type(value = HardConstraint.MinAvailabilityRatio::class, name = "MIN_AVAILABILITY_RATIO"),
    JsonSubTypes.Type(value = HardConstraint.RequireAllAvailable::class, name = "REQUIRE_ALL_AVAILABLE"),
    JsonSubTypes.Type(value = HardConstraint.MaxDurationSlots::class, name = "MAX_DURATION_SLOTS"),
    JsonSubTypes.Type(value = HardConstraint.MaxBlocksPerDay::class, name = "MAX_BLOCKS_PER_DAY"),
)
sealed interface HardConstraint {
    fun isSatisfied(
        candidate: PlacementCandidate,
        env: ScoringEnv,
    ): Boolean

    /** 근무 시간 [startSlot, endSlot) 안에 완전히 포함되어야 함. */
    data class WorkingHours(
        val startSlot: Int,
        val endSlot: Int,
    ) : HardConstraint {
        override fun isSatisfied(
            candidate: PlacementCandidate,
            env: ScoringEnv,
        ): Boolean = candidate.startSlot >= startSlot && candidate.endSlot <= endSlot
    }

    /** boundarySlot 이후(심야)로 넘어가면 안 됨. */
    data class NoLateNight(
        val boundarySlot: Int,
    ) : HardConstraint {
        override fun isSatisfied(
            candidate: PlacementCandidate,
            env: ScoringEnv,
        ): Boolean = candidate.endSlot <= boundarySlot
    }

    /** 가용 멤버 비율이 minRatio 이상이어야 함. */
    data class MinAvailabilityRatio(
        val minRatio: Double,
    ) : HardConstraint {
        override fun isSatisfied(
            candidate: PlacementCandidate,
            env: ScoringEnv,
        ): Boolean = candidate.feasibility.availabilityRatio >= minRatio
    }

    /** 모든 멤버가 가용해야 함(충돌 0). */
    data class RequireAllAvailable(
        val unused: Boolean = true,
    ) : HardConstraint {
        override fun isSatisfied(
            candidate: PlacementCandidate,
            env: ScoringEnv,
        ): Boolean = candidate.feasibility.feasible
    }

    /** 블록 길이가 maxSlots 이하여야 함. */
    data class MaxDurationSlots(
        val maxSlots: Int,
    ) : HardConstraint {
        override fun isSatisfied(
            candidate: PlacementCandidate,
            env: ScoringEnv,
        ): Boolean = candidate.durationSlots <= maxSlots
    }

    /**
     * 하루(같은 날짜)에 배치되는 블록 수를 maxPerDay 이하로 제한한다.
     * 이미 같은 날짜에 maxPerDay 개가 배치(pending)되어 있으면 추가 배치를 막아 일정을 분산시킨다.
     */
    data class MaxBlocksPerDay(
        val maxPerDay: Int,
    ) : HardConstraint {
        override fun isSatisfied(
            candidate: PlacementCandidate,
            env: ScoringEnv,
        ): Boolean {
            if (maxPerDay <= 0) return true
            val sameDayCount = env.pendingBlocks.count { it.date == candidate.date }
            return sameDayCount < maxPerDay
        }
    }
}
