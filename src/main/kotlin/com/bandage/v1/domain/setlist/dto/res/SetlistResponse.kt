package com.bandage.v1.domain.setlist.dto.res

import com.bandage.v1.domain.setlist.model.Setlist
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "셋리스트 응답")
data class SetlistResponse(
    val setlistId: UUID,
    val trackSelectionId: UUID,
    val title: String,
    val managerId: Long,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun of(setlist: Setlist): SetlistResponse =
            SetlistResponse(
                setlistId = setlist.id,
                trackSelectionId = setlist.trackSelectionId,
                title = setlist.title,
                managerId = setlist.managerId,
                createdAt = setlist.createdAt,
                updatedAt = setlist.lastModifiedAt,
            )
    }
}
