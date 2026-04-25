package com.bandage.v1.domain.performance.dto.res

import com.bandage.v1.domain.band.model.Band
import com.bandage.v1.domain.performance.model.Performance
import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "공연 상세 조회 응답")
data class PerformanceDetailResponse(
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
    @Schema(description = "참여 밴드 요약 목록")
    val bands: List<BandSummary>,
    @Schema(description = "매니저 멤버 아이디 목록")
    val managerIds: List<Long>,
    @Schema(description = "연결된 합주 요약 목록")
    val practices: List<PracticeSummary>,
) {
    @Schema(description = "공연 참여 밴드 요약")
    data class BandSummary(
        @Schema(description = "밴드 고유 식별자", example = "550e8400-e29b-41d4-a716-446655440000")
        val bandId: UUID,
        @Schema(description = "밴드 이름", example = "TuNA")
        val bandName: String,
    )

    @Schema(description = "공연 연결 합주 요약")
    data class PracticeSummary(
        @Schema(description = "합주 고유 식별자", example = "550e8400-e29b-41d4-a716-446655440000")
        val practiceId: UUID,
        @Schema(description = "합주 제목", example = "TuNA 정기공연 1주차 합주")
        val title: String,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm", shape = JsonFormat.Shape.STRING, timezone = "Asia/Seoul")
        @Schema(description = "합주 시작 시간", example = "2026-06-01 18:00")
        val startAt: LocalDateTime,
    )

    companion object {
        fun of(
            performance: Performance,
            bands: List<Band>,
        ): PerformanceDetailResponse {
            val bandById = bands.associateBy { it.id }
            return PerformanceDetailResponse(
                performanceId = performance.id,
                title = performance.title,
                startAt = performance.schedule.startAt,
                durationMinutes = performance.schedule.durationMinutes,
                venue = performance.schedule.venue,
                bands =
                    performance.bands.mapNotNull { pb ->
                        bandById[pb.bandId]?.let { BandSummary(bandId = it.id, bandName = it.name) }
                    },
                managerIds = performance.managers.map { it.member },
                practices =
                    performance.practices.map { pp ->
                        PracticeSummary(
                            practiceId = pp.practice.id,
                            title = pp.practice.title,
                            startAt = pp.practice.schedule.startAt,
                        )
                    },
            )
        }
    }
}
