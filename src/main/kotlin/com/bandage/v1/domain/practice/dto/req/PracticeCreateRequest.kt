package com.bandage.v1.domain.practice.dto.req

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "합주 생성 요청")
data class PracticeCreateRequest(
    @Schema(description = "합주 타이틀", example = "TuNA 정기공연 1주차 합주")
    val title: String?,
    @Schema(description = "합주곡 아이디", example = "550e8400-e29b-41d4-a716-446655440000")
    val song: UUID,
    @Schema(description = "합주 타이틀", example = "TuNA")
    val venue: String?,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", shape = JsonFormat.Shape.STRING, timezone = "Asia/Seoul")
    @Schema(description = "합주 시작 시간", example = "2026-03-15 18:00")
    val startAt: LocalDateTime,
    @Schema(description = "합주 시간 (분)", example = "60")
    val durationMinutes: Int,
)
