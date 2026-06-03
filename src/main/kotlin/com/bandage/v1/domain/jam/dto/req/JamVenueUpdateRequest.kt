package com.bandage.v1.domain.jam.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "합주 장소 변경 요청")
data class JamVenueUpdateRequest(
    @NotBlank @Schema(description = "합주 장소", example = "Club FF")
    val venue: String,
)
