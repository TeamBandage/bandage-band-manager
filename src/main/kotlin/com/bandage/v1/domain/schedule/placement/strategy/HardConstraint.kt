package com.bandage.v1.domain.schedule.placement.strategy

import com.bandage.v1.domain.schedule.placement.PlacementCandidate
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * 반드시 만족해야 하는 제약. 하나라도 위반하면 후보는 탈락한다.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = HardConstraint.WorkingHours::class, name = "WORKING_HOURS"),
    JsonSubTypes.Type(value = HardConstraint.NoLateNight::class, name = "NO_LATE_NIGHT"),
    JsonSubTypes.Type(value = HardConstraint.MinAvailabilityRatio::class, name = "MIN_AVAILABILITY_RATIO"),
    JsonSubTypes.Type(value = HardConstraint.RequireAllAvailable::class, name = "REQUIRE_ALL_AVAILABLE"),
    JsonSubTypes.Type(value = HardConstraint.MaxDurationSlots::class, name = "MAX_DURATION_SLOTS"),
)
sealed interface HardConstraint {
    fun isSatisfied(candidate: PlacementCandidate): Boolean

    /** 근무 시간 [startSlot, endSlot) 안에 완전히 포함되어야 함. */
    data class WorkingHours(
        val startSlot: Int,
        val endSlot: Int,
    ) : HardConstraint {
        override fun isSatisfied(candidate: PlacementCandidate): Boolean = candidate.startSlot >= startSlot && candidate.endSlot <= endSlot
    }

    /** boundarySlot 이후(심야)로 넘어가면 안 됨. */
    data class NoLateNight(
        val boundarySlot: Int,
    ) : HardConstraint {
        override fun isSatisfied(candidate: PlacementCandidate): Boolean = candidate.endSlot <= boundarySlot
    }

    /** 가용 멤버 비율이 minRatio 이상이어야 함. */
    data class MinAvailabilityRatio(
        val minRatio: Double,
    ) : HardConstraint {
        override fun isSatisfied(candidate: PlacementCandidate): Boolean = candidate.feasibility.availabilityRatio >= minRatio
    }

    /** 모든 멤버가 가용해야 함(충돌 0). */
    data class RequireAllAvailable(
        val unused: Boolean = true,
    ) : HardConstraint {
        override fun isSatisfied(candidate: PlacementCandidate): Boolean = candidate.feasibility.feasible
    }

    /** 블록 길이가 maxSlots 이하여야 함. */
    data class MaxDurationSlots(
        val maxSlots: Int,
    ) : HardConstraint {
        override fun isSatisfied(candidate: PlacementCandidate): Boolean = candidate.durationSlots <= maxSlots
    }
}
