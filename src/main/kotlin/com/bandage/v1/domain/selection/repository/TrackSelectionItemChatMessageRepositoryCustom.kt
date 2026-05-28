package com.bandage.v1.domain.selection.repository

import com.bandage.v1.domain.selection.model.TrackSelectionItemChatMessage
import com.bandage.v1.global.common.response.CursorResponse
import java.util.UUID

interface TrackSelectionItemChatMessageRepositoryCustom {
    fun findAllByItemAndPaging(
        itemId: UUID,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<TrackSelectionItemChatMessage, UUID>
}
