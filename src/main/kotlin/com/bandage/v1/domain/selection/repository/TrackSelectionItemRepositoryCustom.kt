package com.bandage.v1.domain.selection.repository

import com.bandage.v1.domain.selection.model.TrackSelectionItem
import com.bandage.v1.global.common.response.CursorResponse
import java.util.UUID

interface TrackSelectionItemRepositoryCustom {
    fun findAllBySelectionAndPaging(
        selectionId: UUID,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<TrackSelectionItem, UUID>
}
