package com.bandage.bandmanager.domain.selection.repository

import com.bandage.bandmanager.domain.selection.model.TrackSelectionItemChatMessage
import com.bandage.bandmanager.global.common.response.CursorResponse
import java.util.UUID

interface TrackSelectionItemChatMessageRepositoryCustom {
    fun findAllByItemAndPaging(
        itemId: UUID,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<TrackSelectionItemChatMessage, UUID>
}
