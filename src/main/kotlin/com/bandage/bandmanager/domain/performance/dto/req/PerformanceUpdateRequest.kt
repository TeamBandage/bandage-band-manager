package com.bandage.bandmanager.domain.performance.dto.req

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.Min
import java.time.LocalDateTime

@Schema(description = "공연 정보 수정 요청 (모든 필드 optional, 전달된 필드만 갱신)")
data class PerformanceUpdateRequest(
    @Schema(description = "공연 제목", example = "TuNA 정기공연 (수정)")
    val title: String? = null,
    @field:Future(message = "공연 시작 시간은 현재 이후여야 합니다.")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", shape = JsonFormat.Shape.STRING, timezone = "Asia/Seoul")
    @Schema(description = "공연 시작 시간 (현재 이후만 허용)", example = "2026-06-15 19:00")
    val startAt: LocalDateTime? = null,
    @field:Min(1)
    @Schema(description = "공연 시간 (분)", example = "90")
    val durationMinutes: Int? = null,
    @Schema(description = "공연 장소", example = "Club FF")
    val venue: String? = null,
)
