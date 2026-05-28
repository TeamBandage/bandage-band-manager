package com.bandage.v1.domain.performance.dto.req

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "공연 생성 요청")
data class PerformanceCreateRequest(
    @NotBlank
    @Schema(description = "공연 제목", example = "TuNA 정기공연")
    val title: String,
    @Schema(
        description = "참여 셋리스트 아이디 목록 (optional, 미전달/null 시 빈 배열로 처리)",
        example = "[\"550e8400-e29b-41d4-a716-446655440000\"]",
    )
    val setlistIds: List<UUID>? = null,
    @field:Future(message = "공연 시작 시간은 현재 이후여야 합니다.")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", shape = JsonFormat.Shape.STRING, timezone = "Asia/Seoul")
    @Schema(description = "공연 시작 시간 (현재 이후만 허용)", example = "2026-06-15 18:00")
    val startAt: LocalDateTime,
    @field:Min(1)
    @Schema(description = "공연 시간 (분)", example = "120")
    val durationMinutes: Int,
    @Schema(description = "공연 장소", example = "Club FF")
    val venue: String?,
)
