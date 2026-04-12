package com.bandage.v1.domain.practice.dto.res

import com.bandage.v1.domain.practice.model.Practice
import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "합주 상세 조회 응답")
data class PracticeDetailResponse(
    @Schema(description = "합주 고유 식별자 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val practiceId: UUID,
    @Schema(description = "합주 타이틀", example = "TuNA 정기공연 1주차 합주")
    val title: String,
    @Schema(description = "합주 장소", example = "홍대 스튜디오")
    val venue: String?,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", shape = JsonFormat.Shape.STRING, timezone = "Asia/Seoul")
    @Schema(description = "합주 시작 시간", example = "2026-03-15 18:00")
    val startAt: LocalDateTime,
    @Schema(description = "합주 시간 (분)", example = "60")
    val durationMinutes: Int,
    @Schema(description = "합주곡 정보")
    val song: PracticeSongInfo,
    @Schema(description = "세션 목록")
    val sessions: List<PracticeSessionResponse>,
    @Schema(description = "참여자 목록")
    val participants: List<PracticeParticipantResponse>,
) {
    @Schema(description = "합주곡 정보")
    data class PracticeSongInfo(
        @Schema(description = "합주곡 고유 식별자 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
        val songId: UUID,
        @Schema(description = "곡 제목", example = "Stairway to Heaven")
        val title: String,
        @Schema(description = "아티스트", example = "Led Zeppelin")
        val artist: String,
    )

    companion object {
        fun of(practice: Practice): PracticeDetailResponse =
            PracticeDetailResponse(
                practiceId = practice.id,
                title = practice.title,
                venue = practice.venue,
                startAt = practice.startAt,
                durationMinutes = practice.durationMinutes,
                song =
                    PracticeSongInfo(
                        songId = practice.song.id,
                        title = practice.song.title,
                        artist = practice.song.artist,
                    ),
                sessions = practice.sessions.map { PracticeSessionResponse.of(it) },
                participants = practice.participants.map { PracticeParticipantResponse.of(it) },
            )
    }
}
