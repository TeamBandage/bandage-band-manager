package com.bandage.v1.domain.practice.dto.req

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import java.time.LocalDateTime

@Schema(description = "합주 일정 변경 요청")
data class PracticeScheduleUpdateRequest(
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", shape = JsonFormat.Shape.STRING, timezone = "Asia/Seoul")
    @Schema(description = "합주 시작 시간", example = "2026-03-15 18:00")
    val startAt: LocalDateTime,
    @field:Min(1)
    @Schema(description = "합주 시간 (분)", example = "90")
    val durationMinutes: Int,
)
