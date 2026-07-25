package com.bandage.bandmanager.domain.availability.dto.res

import com.bandage.bandmanager.domain.availability.model.AvailabilityException
import com.bandage.bandmanager.domain.availability.model.AvailabilityKind
import com.bandage.bandmanager.domain.availability.model.MemberAvailability
import com.bandage.bandmanager.domain.availability.model.WeeklyRule
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
            from(availability, availability.weeklyRules, availability.exceptions)

        /** 기간 필터링 등으로 선별한 규칙/예외만 담아 응답한다(memberId/note/updatedAt 는 원본 유지). */
        fun from(
            availability: MemberAvailability,
            weeklyRules: List<WeeklyRule>,
            exceptions: List<AvailabilityException>,
        ): MemberAvailabilityResponse =
            MemberAvailabilityResponse(
                memberId = availability.memberId,
                weeklyRules = weeklyRules.map { WeeklyRuleResponse.from(it) },
                exceptions = exceptions.map { AvailabilityExceptionResponse.from(it) },
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
