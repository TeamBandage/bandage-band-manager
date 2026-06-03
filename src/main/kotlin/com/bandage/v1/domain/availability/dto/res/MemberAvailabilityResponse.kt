package com.bandage.v1.domain.availability.dto.res

import com.bandage.v1.domain.availability.model.AvailabilityException
import com.bandage.v1.domain.availability.model.AvailabilityKind
import com.bandage.v1.domain.availability.model.MemberAvailability
import com.bandage.v1.domain.availability.model.WeeklyRule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

data class MemberAvailabilityResponse(
    val memberId: Long,
    val weeklyRules: List<WeeklyRuleResponse>,
    val exceptions: List<AvailabilityExceptionResponse>,
    val note: String?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun from(availability: MemberAvailability): MemberAvailabilityResponse =
            MemberAvailabilityResponse(
                memberId = availability.memberId,
                weeklyRules = availability.weeklyRules.map { WeeklyRuleResponse.from(it) },
                exceptions = availability.exceptions.map { AvailabilityExceptionResponse.from(it) },
                note = availability.note,
                updatedAt = availability.lastModifiedAt,
            )

        fun empty(memberId: Long): MemberAvailabilityResponse =
            MemberAvailabilityResponse(
                memberId = memberId,
                weeklyRules = emptyList(),
                exceptions = emptyList(),
                note = null,
                updatedAt = null,
            )
    }

    data class WeeklyRuleResponse(
        val dayOfWeek: DayOfWeek,
        val startSlot: Int,
        val endSlot: Int,
        val effectiveFrom: LocalDate,
        val effectiveTo: LocalDate?,
    ) {
        companion object {
            fun from(rule: WeeklyRule): WeeklyRuleResponse =
                WeeklyRuleResponse(
                    dayOfWeek = rule.dayOfWeek,
                    startSlot = rule.startSlot,
                    endSlot = rule.endSlot,
                    effectiveFrom = rule.effectiveFrom,
                    effectiveTo = rule.effectiveTo,
                )
        }
    }

    data class AvailabilityExceptionResponse(
        val date: LocalDate,
        val kind: AvailabilityKind,
        val startSlot: Int?,
        val endSlot: Int?,
    ) {
        companion object {
            fun from(exception: AvailabilityException): AvailabilityExceptionResponse =
                AvailabilityExceptionResponse(
                    date = exception.date,
                    kind = exception.kind,
                    startSlot = exception.startSlot,
                    endSlot = exception.endSlot,
                )
        }
    }
}
