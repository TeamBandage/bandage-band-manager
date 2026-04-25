package com.bandage.v1.domain.practice.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "합주곡 검색 쿼리")
data class PracticeSongSearchQuery(
    @field:NotBlank
    @Schema(description = "검색 키워드 (곡 제목 또는 아티스트)", example = "Tool")
    val keyword: String,
)
