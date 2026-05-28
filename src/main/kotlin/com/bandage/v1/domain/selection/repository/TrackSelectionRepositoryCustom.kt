package com.bandage.v1.domain.selection.repository

import com.bandage.v1.domain.selection.model.TrackSelection
import com.bandage.v1.global.common.response.CursorResponse
import java.util.UUID

interface TrackSelectionRepositoryCustom {
    fun findAllByMemberAndPaging(
        memberId: Long,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<TrackSelection, UUID>
}
