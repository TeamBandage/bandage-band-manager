package com.bandage.v1.domain.selection.dto.res

import com.bandage.v1.domain.selection.model.TrackSelectionItem
import com.bandage.v1.domain.selection.model.TrackSelectionItemApplicant
import com.bandage.v1.domain.selection.model.TrackSelectionItemConfirmation
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "선곡 항목 응답")
data class TrackSelectionItemResponse(
    val trackSelectionItemId: UUID,
    val selectionId: UUID,
    val title: String,
    val artist: String,
    val album: String?,
    val duration: String?,
    val proposerId: Long,
    val note: String?,
    val practiceSongId: UUID?,
    val isSelected: Boolean,
    val sessions: List<SessionDefResponse>,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun of(
            item: TrackSelectionItem,
            applicants: List<TrackSelectionItemApplicant>,
            confirmations: List<TrackSelectionItemConfirmation>,
        ): TrackSelectionItemResponse {
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
            return TrackSelectionItemResponse(
                trackSelectionItemId = item.id,
                selectionId = item.selection.id,
                title = item.title,
                artist = item.artist,
                album = item.album,
                duration = item.duration,
                proposerId = item.proposerId,
                note = item.note,
                practiceSongId = item.practiceSongId,
                isSelected = item.isSelected,
                sessions = sessionResponses,
                createdAt = item.createdAt,
                updatedAt = item.lastModifiedAt,
            )
        }
    }
}
