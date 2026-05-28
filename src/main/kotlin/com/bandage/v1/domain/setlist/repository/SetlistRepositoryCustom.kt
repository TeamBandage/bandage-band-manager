package com.bandage.v1.domain.setlist.repository

import com.bandage.v1.domain.setlist.model.Setlist
import com.bandage.v1.global.common.response.CursorResponse
import java.util.UUID

interface SetlistRepositoryCustom {
    fun findAllByMemberAndPaging(
        memberId: Long,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Setlist, UUID>

    fun findAllAccessibleByTitle(
        title: String,
        memberId: Long,
    ): List<Setlist>

    fun isAccessibleMember(
        setlistId: UUID,
        memberId: Long,
    ): Boolean
}
