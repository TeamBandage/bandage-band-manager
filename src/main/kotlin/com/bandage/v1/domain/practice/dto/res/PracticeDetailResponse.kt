package com.bandage.v1.domain.practice.dto.res

import com.bandage.v1.domain.practice.model.Practice
import com.bandage.v1.domain.practice.model.PracticeParticipant
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
    @Schema(description = "원본 셋리스트 ID (Setlist 경유 생성 시). 독립 생성 시 null")
    val setlistId: UUID?,
    @Schema(description = "합주 장소", example = "Club FF")
    val venue: String?,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", shape = JsonFormat.Shape.STRING, timezone = "Asia/Seoul")
    @Schema(description = "합주 시작 시간", example = "2026-03-15 18:00")
    val startAt: LocalDateTime,
    @Schema(description = "합주 시간 (분)", example = "60")
    val durationMinutes: Int,
    @Schema(description = "합주 메모")
    val note: String?,
    @Schema(description = "곡 정보")
    val track: TrackInfoResponse,
    @Schema(description = "세션 목록")
    val sessions: List<PracticeSessionResponse>,
    @Schema(description = "참여자 목록")
    val participants: List<PracticeParticipantResponse>,
) {
    @Schema(description = "곡 정보")
    data class TrackInfoResponse(
        @Schema(description = "곡 제목", example = "Stairway to Heaven")
        val title: String,
        @Schema(description = "아티스트", example = "Led Zeppelin")
        val artist: String,
        @Schema(description = "앨범", example = "Led Zeppelin IV")
        val album: String?,
        @Schema(description = "곡 길이(초 단위). 분/초(mm:ss) 표시 변환은 클라이언트에서 처리", example = "482")
        val duration: Int?,
        @Schema(description = "참고 링크(예: YouTube)")
        val reference: String?,
    )

    companion object {
        fun of(practice: Practice): PracticeDetailResponse {
            val participantsBySession: Map<String, List<PracticeParticipant>> = practice.participants.groupBy { it.sessionId }
            return PracticeDetailResponse(
                practiceId = practice.id,
                title = practice.title,
                setlistId = practice.setlistId,
                venue = practice.timeInfo.venue,
                startAt = practice.timeInfo.startAt,
                durationMinutes = practice.timeInfo.durationMinutes,
                note = practice.note,
                track =
                    TrackInfoResponse(
                        title = practice.trackInfo.title,
                        artist = practice.trackInfo.artist,
                        album = practice.trackInfo.album,
                        duration = practice.trackInfo.duration,
                        reference = practice.trackInfo.reference,
                    ),
                sessions =
                    practice.sessions.map { def ->
                        PracticeSessionResponse.of(
                            def = def,
                            participants = participantsBySession[def.sessionId]?.map { it.member } ?: emptyList(),
                        )
                    },
                participants = practice.participants.map { PracticeParticipantResponse.of(it) },
            )
        }
    }
}
