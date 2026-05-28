package com.bandage.v1.domain.selection.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "선곡 항목 선택/해제 요청")
data class TrackSelectionItemSelectionRequest(
    @field:NotNull
    @Schema(description = "true=선택, false=해제")
    val isSelected: Boolean,
)
