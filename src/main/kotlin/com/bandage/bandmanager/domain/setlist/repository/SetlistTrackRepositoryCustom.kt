package com.bandage.bandmanager.domain.setlist.repository

import com.bandage.bandmanager.domain.setlist.model.SetlistTrack
import com.bandage.bandmanager.global.common.response.CursorResponse
import java.util.UUID

interface SetlistTrackRepositoryCustom {
    fun findAllBySetlistAndPaging(
        setlistId: UUID,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<SetlistTrack, UUID>
}
