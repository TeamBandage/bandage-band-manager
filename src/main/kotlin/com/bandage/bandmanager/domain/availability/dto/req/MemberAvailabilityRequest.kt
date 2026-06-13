package com.bandage.bandmanager.domain.availability.dto.req

import com.bandage.bandmanager.domain.availability.model.AvailabilityException
import com.bandage.bandmanager.domain.availability.model.AvailabilityKind
import com.bandage.bandmanager.domain.availability.model.WeeklyRule
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 멤버 글로벌 가용성 등록/수정(upsert) 요청.
 * weeklyRules/exceptions 는 전체 교체(replace) 방식으로 적용된다.
 */
data class MemberAvailabilityRequest(
    @field:Valid
    val weeklyRules: List<WeeklyRuleRequest> = emptyList(),
    @field:Valid
    val exceptions: List<AvailabilityExceptionRequest> = emptyList(),
    @field:Size(max = 500, message = "note 는 최대 500자까지 입력할 수 있습니다.")
    val note: String? = null,
) {
    fun toWeeklyRules(): List<WeeklyRule> =
        weeklyRules.map {
            WeeklyRule(
                dayOfWeek = it.dayOfWeek,
                startSlot = it.startSlot,
                endSlot = it.endSlot,
                effectiveFrom = it.effectiveFrom,
                effectiveTo = it.effectiveTo,
            )
        }

    fun toExceptions(): List<AvailabilityException> =
        exceptions.map {
            AvailabilityException(
                date = it.date,
                kind = it.kind,
                startSlot = it.startSlot,
                endSlot = it.endSlot,
            )
        }

    data class WeeklyRuleRequest(
        val dayOfWeek: DayOfWeek,
        val startSlot: Int,
        val endSlot: Int,
        val effectiveFrom: LocalDate,
        val effectiveTo: LocalDate? = null,
    )

    data class AvailabilityExceptionRequest(
        val date: LocalDate,
        val kind: AvailabilityKind,
        val startSlot: Int? = null,
        val endSlot: Int? = null,
    )
}
