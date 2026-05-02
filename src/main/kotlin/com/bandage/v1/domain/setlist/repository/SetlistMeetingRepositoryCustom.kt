package com.bandage.v1.domain.setlist.repository

import com.bandage.v1.domain.setlist.model.SetlistMeeting
import com.bandage.v1.global.common.response.CursorResponse
import java.util.UUID

interface SetlistMeetingRepositoryCustom {
    fun findAllByMemberAndPaging(
        memberId: Long,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<SetlistMeeting, UUID>
}
