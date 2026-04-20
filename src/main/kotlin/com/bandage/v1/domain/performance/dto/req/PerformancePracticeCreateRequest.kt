package com.bandage.v1.domain.performance.dto.req

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "공연 합주곡 생성 및 추가 요청")
data class PerformancePracticeCreateRequest(
    @Schema(description = "합주 제목 (미입력 시 곡 제목으로 대체)", example = "TuNA 정기공연 합주")
    val title: String?,
    @Schema(description = "합주곡 아이디", example = "550e8400-e29b-41d4-a716-446655440000")
    val songId: UUID,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", shape = JsonFormat.Shape.STRING, timezone = "Asia/Seoul")
    @Schema(description = "합주 시작 시간", example = "2026-06-01 18:00")
    val startAt: LocalDateTime,
    @field:Min(1)
    @Schema(description = "합주 시간 (분)", example = "60")
    val durationMinutes: Int,
    @Schema(description = "합주 장소", example = "홍대 스튜디오")
    val venue: String?,
)
