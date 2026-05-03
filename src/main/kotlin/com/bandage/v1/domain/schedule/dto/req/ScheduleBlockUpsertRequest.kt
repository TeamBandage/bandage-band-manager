package com.bandage.v1.domain.schedule.dto.req

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.util.UUID

data class ScheduleBlockUpsertRequest(
    @field:NotNull(message = "songId 는 필수입니다.")
    val songId: UUID,
    @field:NotNull(message = "date 는 필수입니다.")
    val date: LocalDate,
    @field:NotNull
    @field:Min(value = 0, message = "startSlot 은 0 이상이어야 합니다.")
    val startSlot: Int,
    @field:NotNull
    @field:Min(value = 1, message = "durationSlots 는 1 이상이어야 합니다.")
    val durationSlots: Int,
    val pinned: Boolean? = null,
    val paletteIndex: Int? = null,
    val songTitleOverride: String? = null,
    @field:Size(max = 200, message = "note 는 최대 200자까지 입력할 수 있습니다.")
    val note: String? = null,
)
