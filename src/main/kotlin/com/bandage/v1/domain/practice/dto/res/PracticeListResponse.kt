package com.bandage.v1.domain.practice.dto.res

import com.bandage.v1.domain.practice.model.Practice
import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "합주 목록 조회 응답")
data class PracticeListResponse(
    @Schema(description = "합주 고유 식별자 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val practiceId: UUID,
    @Schema(description = "합주 타이틀", example = "TuNA 정기공연 1주차 합주")
    val title: String,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", shape = JsonFormat.Shape.STRING, timezone = "Asia/Seoul")
    @Schema(description = "합주 시작 시간", example = "2026-03-15 18:00")
    val startAt: LocalDateTime,
    @Schema(description = "합주 시간 (분)", example = "60")
    val durationMinutes: Int,
    @Schema(description = "합주 장소", example = "홍대 스튜디오")
    val venue: String?,
) {
    companion object {
        fun of(practice: Practice): PracticeListResponse =
            PracticeListResponse(
                practiceId = practice.id,
                title = practice.title,
                startAt = practice.timeInfo.startAt,
                durationMinutes = practice.timeInfo.durationMinutes,
                venue = practice.timeInfo.venue,
            )
    }
}
