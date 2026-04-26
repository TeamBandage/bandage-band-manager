package com.bandage.v1.domain.practice.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.util.UUID

@Schema(description = "합주곡 생성 요청 (필드 직접 입력 — 자작곡 등 외부 API 연동이 필요 없는 곡 등록용)")
data class PracticeSongCreateRequest(
    @Schema(
        description = "합주 ID (optional, 1:1 바인딩 대상). 미제공 시 PracticeSong 만 생성하고 Practice 바인딩은 수행하지 않음.",
        example = "550e8400-e29b-41d4-a716-446655440000",
    )
    val practiceId: UUID? = null,
    @field:NotBlank
    @Schema(description = "곡 제목", example = "자작곡 No.1")
    val title: String,
    @field:NotBlank
    @Schema(description = "아티스트", example = "TuNA")
    val artist: String,
    @field:NotBlank
    @Schema(description = "앨범", example = "TuNA Demo")
    val album: String,
    @field:Min(1)
    @Schema(description = "곡 길이 (초)", example = "300")
    val duration: Int,
    @Schema(description = "참조 링크", example = "https://www.youtube.com/watch?v=example")
    val refLink: String? = null,
)
