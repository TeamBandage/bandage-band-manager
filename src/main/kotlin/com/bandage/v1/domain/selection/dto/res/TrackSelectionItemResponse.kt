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
    @Schema(description = "곡 길이(초 단위). 분/초(mm:ss) 표시 변환은 클라이언트에서 처리")
    val duration: Int?,
    @Schema(description = "참고 링크(예: YouTube)")
    val reference: String?,
    val proposerId: Long,
    val note: String?,
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
                title = item.trackInfo.title,
                artist = item.trackInfo.artist,
                album = item.trackInfo.album,
                duration = item.trackInfo.duration,
                reference = item.trackInfo.reference,
                proposerId = item.proposerId,
                note = item.note,
                isSelected = item.isSelected,
                sessions = sessionResponses,
                createdAt = item.createdAt,
                updatedAt = item.lastModifiedAt,
            )
        }
    }
}
