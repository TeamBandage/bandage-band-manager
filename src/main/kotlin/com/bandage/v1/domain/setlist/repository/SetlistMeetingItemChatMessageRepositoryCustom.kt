package com.bandage.v1.domain.setlist.repository

import com.bandage.v1.domain.setlist.model.SetlistMeetingItemChatMessage
import com.bandage.v1.global.common.response.CursorResponse
import java.util.UUID

interface SetlistMeetingItemChatMessageRepositoryCustom {
    fun findAllByItemAndPaging(
        itemId: UUID,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<SetlistMeetingItemChatMessage, UUID>
}
