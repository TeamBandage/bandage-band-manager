package com.bandage.bandmanager.domain.setlist.repository

import com.bandage.bandmanager.domain.setlist.model.Setlist
import com.bandage.bandmanager.global.common.response.CursorResponse
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
