package com.bandage.v1.domain.practice.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

@Schema(description = "외부 시스템 곡 정보 (전송 전용 DTO, DB 엔티티로 관리되지 않음)")
data class Song(
    @field:NotBlank
    @Schema(description = "곡 제목", example = "Vicarious")
    val title: String,
    @field:NotBlank
    @Schema(description = "아티스트", example = "Tool")
    val artist: String,
    @field:NotBlank
    @Schema(description = "앨범", example = "10,000 Days")
    val album: String,
    @field:Min(1)
    @Schema(description = "곡 길이 (초)", example = "426")
    val duration: Int,
    @Schema(description = "참조 링크", example = "https://www.youtube.com/watch?v=example")
    val refLink: String? = null,
)
