package com.bandage.bandmanager.domain.selection.repository

import com.bandage.bandmanager.domain.selection.model.TrackSelectionItem
import com.bandage.bandmanager.global.common.response.CursorResponse
import java.util.UUID

interface TrackSelectionItemRepositoryCustom {
    fun findAllBySelectionAndPaging(
        selectionId: UUID,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<TrackSelectionItem, UUID>
}
