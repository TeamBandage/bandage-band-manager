package com.bandage.v1.domain.selection.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

@Schema(description = "선곡 항목 생성 요청")
data class TrackSelectionItemCreateRequest(
    @field:NotBlank
    @Schema(description = "곡 제목")
    val title: String,
    @field:NotBlank
    @Schema(description = "아티스트")
    val artist: String,
    @Schema(description = "앨범")
    val album: String?,
    @Schema(description = "재생 시간(mm:ss)")
    val duration: String?,
    @Schema(description = "추천자 의견")
    val note: String?,
    @field:Valid
    @Schema(description = "세션 정의 목록")
    val sessions: List<SessionDefDto> = emptyList(),
)
