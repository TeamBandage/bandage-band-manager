package com.bandage.bandmanager.domain.availability.dto.req

import com.bandage.bandmanager.domain.availability.model.AvailabilityException
import com.bandage.bandmanager.domain.availability.model.AvailabilityKind
import com.bandage.bandmanager.domain.availability.model.WeeklyRule
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 멤버 글로벌 가용성 등록/수정 요청 (범위 한정).
 *
 * [effectiveFrom, effectiveTo] 구간만 교체한다. 이 구간 밖의 가용성(과거 포함)은 보존된다.
 * weeklyRules 는 이 구간 동안 반복 적용되며, exceptions 의 날짜는 반드시 이 구간 안이어야 한다.
 */
data class MemberAvailabilityRequest(
    @Schema(description = "교체 대상 구간 시작일", example = "2026-06-13")
    val effectiveFrom: LocalDate,
    @Schema(description = "교체 대상 구간 종료일(포함)", example = "2026-07-01")
    val effectiveTo: LocalDate,
    @field:Valid
    val weeklyRules: List<WeeklyRuleRequest> = emptyList(),
    @field:Valid
    val exceptions: List<AvailabilityExceptionRequest> = emptyList(),
    @field:Size(max = 500, message = "note 는 최대 500자까지 입력할 수 있습니다.")
    val note: String? = null,
) {
    /** 각 주간 규칙은 요청 구간 [effectiveFrom, effectiveTo] 동안 유효하다. */
    fun toWeeklyRules(): List<WeeklyRule> =
        weeklyRules.map {
            WeeklyRule(
                dayOfWeek = it.dayOfWeek,
                startSlot = it.startSlot,
                endSlot = it.endSlot,
                effectiveFrom = effectiveFrom,
                effectiveTo = effectiveTo,
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
    )

    data class AvailabilityExceptionRequest(
        val date: LocalDate,
        val kind: AvailabilityKind,
        val startSlot: Int? = null,
        val endSlot: Int? = null,
    )
}
