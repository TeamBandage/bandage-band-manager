package com.bandage.v1.domain.practice.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min

@Schema(description = "합주곡 수정 요청 (부분 수정)")
data class PracticeSongUpdateRequest(
    @Schema(description = "곡 제목", example = "Vicarious")
    val title: String? = null,
    @Schema(description = "아티스트", example = "Tool")
    val artist: String? = null,
    @Schema(description = "앨범", example = "10,000 Days")
    val album: String? = null,
    @field:Min(1)
    @Schema(description = "곡 길이 (초)", example = "426")
    val duration: Int? = null,
    @Schema(description = "참조 링크", example = "https://www.youtube.com/watch?v=example")
    val refLink: String? = null,
)
