package com.bandage.bandmanager.domain.schedule_bak.dto.req

import com.bandage.bandmanager.domain.schedule_bak.dto.ScheduleBoardConstraintsDto
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class ScheduleBoardCreateRequest(
    @field:NotBlank(message = "name 은 필수입니다.")
    @field:Size(max = 50, message = "name 은 최대 50자까지 입력할 수 있습니다.")
    val name: String,
    val paletteSeed: Int? = null,
    val constraints: ScheduleBoardConstraintsDto? = null,
    // 보드 레벨 연습 가능 날짜 범위(선택). 미지정 시 Performance 기준.
    val windowFrom: LocalDate? = null,
    val windowTo: LocalDate? = null,
)
