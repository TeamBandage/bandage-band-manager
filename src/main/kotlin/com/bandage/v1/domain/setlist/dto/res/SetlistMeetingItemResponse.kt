package com.bandage.v1.domain.setlist.dto.res

import com.bandage.v1.domain.setlist.model.SetlistMeetingItem
import com.bandage.v1.domain.setlist.model.SetlistMeetingItemApplicant
import com.bandage.v1.domain.setlist.model.SetlistMeetingItemConfirmation
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "선곡 회의 항목 응답")
data class SetlistMeetingItemResponse(
    val setlistMeetingItemId: UUID,
    val meetingId: UUID,
    val title: String,
    val artist: String,
    val album: String?,
    val duration: String?,
    val proposerId: Long,
    val note: String?,
    val practiceSongId: UUID?,
    val sessions: List<SessionDefResponse>,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun of(
            item: SetlistMeetingItem,
            applicants: List<SetlistMeetingItemApplicant>,
            confirmations: List<SetlistMeetingItemConfirmation>,
        ): SetlistMeetingItemResponse {
            val applicantsBySession = applicants.groupBy { it.sessionId }
            val confirmedBySession = confirmations.groupBy { it.sessionId }
            val sessionResponses =
                item.sessions.map { def ->
                    SessionDefResponse.of(
                        def = def,
                        applicants = applicantsBySession[def.sessionId]?.map { it.memberId } ?: emptyList(),
                        confirmed = confirmedBySession[def.sessionId]?.map { it.memberId } ?: emptyList(),
                    )
                }
            return SetlistMeetingItemResponse(
                setlistMeetingItemId = item.id,
                meetingId = item.meeting.id,
                title = item.title,
                artist = item.artist,
                album = item.album,
                duration = item.duration,
                proposerId = item.proposerId,
                note = item.note,
                practiceSongId = item.practiceSongId,
                sessions = sessionResponses,
                createdAt = item.createdAt,
                updatedAt = item.lastModifiedAt,
            )
        }
    }
}
