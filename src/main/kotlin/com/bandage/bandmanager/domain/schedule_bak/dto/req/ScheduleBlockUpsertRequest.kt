package com.bandage.bandmanager.domain.schedule_bak.dto.req

import com.bandage.bandmanager.domain.schedule_bak.model.RecurrenceFreq
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.util.UUID

data class ScheduleBlockUpsertRequest(
    @field:NotEmpty(message = "trackIds 는 최소 1개 이상이어야 합니다.")
    val trackIds: List<UUID>,
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
    val titleOverride: String? = null,
    @field:Size(max = 200, message = "note 는 최대 200자까지 입력할 수 있습니다.")
    val note: String? = null,
    val recurrence: RecurrenceRequest? = null,
) {
    data class RecurrenceRequest(
        val freq: RecurrenceFreq = RecurrenceFreq.NONE,
        @field:Min(value = 1, message = "interval 은 1 이상이어야 합니다.")
        val interval: Int = 1,
        val until: LocalDate? = null,
        val count: Int? = null,
    )
}
