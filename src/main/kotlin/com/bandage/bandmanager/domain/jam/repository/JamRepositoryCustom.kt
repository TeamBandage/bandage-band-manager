package com.bandage.bandmanager.domain.jam.repository

import com.bandage.bandmanager.domain.jam.model.Jam
import com.bandage.bandmanager.global.common.response.CursorResponse
import java.time.LocalDate
import java.util.UUID

interface JamRepositoryCustom {
    fun findAllByPaging(
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Jam, UUID>

    fun findAllByMembersAndPaging(
        memberIds: List<Long>,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Jam, UUID>

    fun findAllByMemberAndPaging(
        memberId: Long,
        lastId: UUID?,
        pageSize: Int,
        from: LocalDate? = null,
        to: LocalDate? = null,
    ): CursorResponse<Jam, UUID>

    fun searchByMemberAndKeywordAndPaging(
        memberId: Long,
        keyword: String,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Jam, UUID>
}
