package com.bandage.v1.domain.setlist.dto.res

import com.bandage.v1.domain.setlist.model.SetlistTrack
import com.bandage.v1.domain.setlist.model.SetlistTrackParticipant
import com.bandage.v1.global.common.domain.SessionDef
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
    val duration: String?,
    val note: String?,
    val practiceSongId: UUID?,
    val sessions: List<SetlistTrackSessionResponse>,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun of(
            track: SetlistTrack,
            participants: List<SetlistTrackParticipant>,
        ): SetlistTrackResponse {
            val participantsBySession = participants.groupBy { it.sessionId }
            val sessions =
                track.sessions.map { def ->
                    SetlistTrackSessionResponse.of(
                        def = def,
                        participants = participantsBySession[def.sessionId]?.map { it.memberId } ?: emptyList(),
                    )
                }
            return SetlistTrackResponse(
                setlistTrackId = track.id,
                setlistId = track.setlist.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                duration = track.duration,
                note = track.note,
                practiceSongId = track.practiceSongId,
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
    val need: Int,
    val custom: Boolean,
    val participants: List<Long>,
) {
    companion object {
        fun of(
            def: SessionDef,
            participants: List<Long>,
        ): SetlistTrackSessionResponse =
            SetlistTrackSessionResponse(
                sessionId = def.sessionId,
                label = def.label,
                short = def.short,
                need = def.need,
                custom = def.custom,
                participants = participants,
            )
    }
}
