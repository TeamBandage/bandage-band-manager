package com.bandage.bandmanager.domain.jam.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "합주 세션 개별 수정 요청")
data class JamSessionUpdateRequest(
    @field:NotBlank
    @Schema(description = "세션 이름", example = "기타")
    val label: String,
    @field:NotBlank
    @Schema(description = "표시용 약어", example = "G")
    val short: String,
)
