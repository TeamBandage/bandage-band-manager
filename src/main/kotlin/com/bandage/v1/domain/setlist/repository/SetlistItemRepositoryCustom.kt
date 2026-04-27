package com.bandage.v1.domain.setlist.repository

import com.bandage.v1.domain.setlist.model.SetlistItem
import com.bandage.v1.global.common.response.CursorResponse
import java.util.UUID

interface SetlistItemRepositoryCustom {
    fun findAllByMeetingAndPaging(
        meetingId: UUID,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<SetlistItem, UUID>
}
