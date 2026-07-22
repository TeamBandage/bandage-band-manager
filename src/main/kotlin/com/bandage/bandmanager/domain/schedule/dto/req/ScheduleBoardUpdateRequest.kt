package com.bandage.bandmanager.domain.schedule.dto.req

import com.bandage.bandmanager.domain.schedule.dto.ScheduleBoardConstraintsDto
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class ScheduleBoardUpdateRequest(
    @field:Size(max = 50, message = "name 은 최대 50자까지 입력할 수 있습니다.")
    val name: String? = null,
    val constraints: ScheduleBoardConstraintsDto? = null,
    val windowFrom: LocalDate? = null,
    val windowTo: LocalDate? = null,
)
