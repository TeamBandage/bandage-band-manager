package com.bandage.bandmanager.domain.schedule_bak.dto.req

import jakarta.validation.constraints.NotNull

data class ScheduleBlockPinRequest(
    @field:NotNull(message = "pinned 는 필수입니다.")
    val pinned: Boolean,
)
