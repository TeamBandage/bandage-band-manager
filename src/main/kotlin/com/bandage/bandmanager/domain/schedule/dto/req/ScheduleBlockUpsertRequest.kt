package com.bandage.bandmanager.domain.schedule.dto.req

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.util.UUID

data class ScheduleBlockUpsertRequest(
    @field:NotEmpty(message = "trackIds 는 최소 1개 이상이어야 합니다.")
    val trackIds: List<UUID>,
    @field:NotNull(message = "startDate 는 필수입니다.")
    val startDate: LocalDate,
    @field:Max(value = 47, message = "startSlot 은 47 이하이어야 합니다.")
    @field:Min(value = 0, message = "startSlot 은 0 이상이어야 합니다.")
    val startSlot: Int,
    @field:NotNull(message = "endDate 는 필수입니다.")
    val endDate: LocalDate,
    // 반열린 구간의 끝이므로 1..48. 48 이면 당일 24:00 을 뜻한다.
    @field:Max(value = 48, message = "endSlot 은 48 이하이어야 합니다.")
    @field:Min(value = 1, message = "endSlot 은 1 이상이어야 합니다.")
    val endSlot: Int,
    val pinned: Boolean? = null,
    val title: String? = null,
    @field:Size(max = 200, message = "note 는 최대 200자까지 입력할 수 있습니다.")
    val note: String? = null,
)
