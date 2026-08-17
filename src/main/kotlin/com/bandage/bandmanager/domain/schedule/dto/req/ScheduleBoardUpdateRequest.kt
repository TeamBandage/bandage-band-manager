package com.bandage.bandmanager.domain.schedule.dto.req

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class ScheduleBoardUpdateRequest(
    @field:Size(max = 50, message = "name 은 최대 50자까지 입력할 수 있습니다.")
    val name: String? = null,
    // 부분 갱신을 위해 nullable. 한쪽만 보내면 나머지는 기존 값을 유지한다.
    @field:Min(value = 0, message = "boardTimeRangeFrom 은 0 이상이어야 합니다.")
    @field:Max(value = 47, message = "boardTimeRangeFrom 은 47 이하이어야 합니다.")
    val boardTimeRangeFrom: Int? = null,
    @field:Min(value = 1, message = "boardTimeRangeTo 는 1 이상이어야 합니다.")
    @field:Max(value = 48, message = "boardTimeRangeTo 는 48 이하이어야 합니다.")
    val boardTimeRangeTo: Int? = null,
    val windowFrom: LocalDate? = null,
    val windowTo: LocalDate? = null,
)
