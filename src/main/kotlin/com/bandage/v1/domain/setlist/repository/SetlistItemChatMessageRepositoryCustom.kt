package com.bandage.v1.domain.setlist.repository

import com.bandage.v1.domain.setlist.model.SetlistItemChatMessage
import com.bandage.v1.global.common.response.CursorResponse
import java.util.UUID

interface SetlistItemChatMessageRepositoryCustom {
    fun findAllByItemAndPaging(
        itemId: UUID,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<SetlistItemChatMessage, UUID>
}
