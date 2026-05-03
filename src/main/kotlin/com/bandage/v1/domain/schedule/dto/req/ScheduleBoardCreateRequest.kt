package com.bandage.v1.domain.schedule.dto.req

import com.bandage.v1.domain.schedule.dto.ScheduleBoardConstraintsDto
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ScheduleBoardCreateRequest(
    @field:NotBlank(message = "name 은 필수입니다.")
    @field:Size(max = 50, message = "name 은 최대 50자까지 입력할 수 있습니다.")
    val name: String,
    val paletteSeed: Int? = null,
    val constraints: ScheduleBoardConstraintsDto? = null,
)
