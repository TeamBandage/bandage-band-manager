package com.bandage.bandmanager.domain.setlist.dto.res

import com.bandage.bandmanager.domain.setlist.model.Setlist
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "셋리스트 상세 응답")
data class SetlistDetailResponse(
    val setlistId: UUID,
    val trackSelectionId: UUID,
    val title: String,
    val managerId: Long,
    val bandIds: List<UUID>,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun of(
            setlist: Setlist,
            bandIds: List<UUID>,
        ): SetlistDetailResponse =
            SetlistDetailResponse(
                setlistId = setlist.id,
                trackSelectionId = setlist.trackSelectionId,
                title = setlist.title,
                managerId = setlist.managerId,
                bandIds = bandIds,
                createdAt = setlist.createdAt,
                updatedAt = setlist.lastModifiedAt,
            )
    }
}
