package com.bandage.bandmanager.domain.selection.dto.res

import com.bandage.bandmanager.domain.selection.model.TrackSelection
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "선곡 응답")
data class TrackSelectionResponse(
    val selectionId: UUID,
    val bandIds: List<UUID>,
    val title: String,
    val managerId: Long,
    val lockedAt: LocalDateTime?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun of(
            m: TrackSelection,
            bandIds: List<UUID>,
        ): TrackSelectionResponse =
            TrackSelectionResponse(
                selectionId = m.id,
                bandIds = bandIds,
                title = m.title,
                managerId = m.managerId,
                lockedAt = m.lockedAt,
                createdAt = m.createdAt,
                updatedAt = m.lastModifiedAt,
            )
    }
}
