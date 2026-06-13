package com.bandage.bandmanager.domain.selection.repository

import com.bandage.bandmanager.domain.selection.model.TrackSelection
import com.bandage.bandmanager.global.common.response.CursorResponse
import java.util.UUID

interface TrackSelectionRepositoryCustom {
    fun findAllByMemberAndPaging(
        memberId: Long,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<TrackSelection, UUID>
}
