package com.bandage.bandmanager.domain.setlist.dto.res

import com.bandage.bandmanager.domain.member.dto.res.MemberSummary
import com.bandage.bandmanager.domain.setlist.model.SetlistTrack
import com.bandage.bandmanager.domain.setlist.model.SetlistTrackParticipant
import com.bandage.bandmanager.global.common.domain.SessionDef
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "셋리스트 트랙 응답")
data class SetlistTrackResponse(
    val setlistTrackId: UUID,
    val setlistId: UUID,
    val title: String,
    val artist: String,
    val album: String?,
    @Schema(description = "곡 길이(초 단위). 분/초(mm:ss) 표시 변환은 클라이언트에서 처리")
    val duration: Int?,
    @Schema(description = "참고 링크(예: YouTube)")
    val reference: String?,
    val note: String?,
    val sessions: List<SetlistTrackSessionResponse>,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun of(
            track: SetlistTrack,
            participants: List<SetlistTrackParticipant>,
            memberInfos: Map<Long, MemberSummary>,
        ): SetlistTrackResponse {
            val participantsBySession = participants.groupBy { it.sessionId }
            val sessions =
                track.sessions.map { def ->
                    SetlistTrackSessionResponse.of(
                        def = def,
                        participants = participantsBySession[def.sessionId]?.mapNotNull { memberInfos[it.memberId] } ?: emptyList(),
                    )
                }
            return SetlistTrackResponse(
                setlistTrackId = track.id,
                setlistId = track.setlist.id,
                title = track.trackInfo.title,
                artist = track.trackInfo.artist,
                album = track.trackInfo.album,
                duration = track.trackInfo.duration,
                reference = track.trackInfo.reference,
                note = track.note,
                sessions = sessions,
                createdAt = track.createdAt,
                updatedAt = track.lastModifiedAt,
            )
        }
    }
}

@Schema(description = "셋리스트 트랙 세션 응답")
data class SetlistTrackSessionResponse(
    val sessionId: String,
    val label: String,
    val short: String,
    val custom: Boolean,
    @Schema(description = "배정된 참여자 목록")
    val participants: List<MemberSummary>,
) {
    companion object {
        fun of(
            def: SessionDef,
            participants: List<MemberSummary>,
        ): SetlistTrackSessionResponse =
            SetlistTrackSessionResponse(
                sessionId = def.sessionId,
                label = def.label,
                short = def.short,
                custom = def.custom,
                participants = participants,
            )
    }
}
