package com.bandage.v1.domain.performance.dto.res

import com.bandage.v1.domain.performance.model.Performance
import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "공연 목록 조회 응답")
data class PerformanceListResponse(
    @Schema(description = "공연 고유 식별자 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val performanceId: UUID,
    @Schema(description = "공연 제목", example = "TuNA 정기공연")
    val title: String,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", shape = JsonFormat.Shape.STRING, timezone = "Asia/Seoul")
    @Schema(description = "공연 시작 시간", example = "2026-06-15 18:00")
    val startAt: LocalDateTime,
    @Schema(description = "공연 시간 (분)", example = "120")
    val durationMinutes: Int,
    @Schema(description = "공연 장소", example = "Club FF")
    val venue: String?,
    @Schema(description = "참여 밴드 + 소속 멤버 목록")
    val bands: List<PerformanceBandSummary>,
) {
    companion object {
        fun of(
            performance: Performance,
            bandSummariesByBandId: Map<UUID, PerformanceBandSummary>,
        ): PerformanceListResponse =
            PerformanceListResponse(
                performanceId = performance.id,
                title = performance.title,
                startAt = performance.timeInfo.startAt,
                durationMinutes = performance.timeInfo.durationMinutes,
                venue = performance.timeInfo.venue,
                bands = performance.bands.mapNotNull { pb -> bandSummariesByBandId[pb.bandId] },
            )
    }
}
