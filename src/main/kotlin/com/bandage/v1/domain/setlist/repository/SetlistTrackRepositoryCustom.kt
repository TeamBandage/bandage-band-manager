package com.bandage.v1.domain.setlist.repository

import com.bandage.v1.domain.setlist.model.SetlistTrack
import com.bandage.v1.global.common.response.CursorResponse
import java.util.UUID

interface SetlistTrackRepositoryCustom {
    fun findAllBySetlistAndPaging(
        setlistId: UUID,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<SetlistTrack, UUID>
}
