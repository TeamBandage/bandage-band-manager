package com.bandage.v1.domain.practice.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "합주곡 참조 링크 upsert 요청")
data class PracticeSongRefLinkUpsertRequest(
    @field:NotBlank
    @Schema(description = "참조 링크 URL", example = "https://www.youtube.com/watch?v=example")
    val refLink: String,
)
