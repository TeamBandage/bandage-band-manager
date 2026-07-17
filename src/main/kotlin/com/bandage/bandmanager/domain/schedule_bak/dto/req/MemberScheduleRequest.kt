package com.bandage.bandmanager.domain.schedule_bak.dto.req

import jakarta.validation.constraints.Size
import java.time.LocalDate

data class MemberScheduleRequest(
    val availableDates: Set<LocalDate>? = null,
    val unavailableDates: Set<LocalDate>? = null,
    val blocks: Map<LocalDate, String>? = null,
    @field:Size(max = 500, message = "note 는 최대 500자까지 입력할 수 있습니다.")
    val note: String? = null,
    val completed: Boolean? = null,
)
