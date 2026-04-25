package com.bandage.v1.domain.practice.repository

import com.bandage.v1.domain.practice.model.Practice
import com.bandage.v1.global.common.response.CursorResponse
import java.util.UUID

interface PracticeRepositoryCustom {
    fun findAllByPaging(
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Practice, UUID>

    fun findAllByMembersAndPaging(
        memberIds: List<Long>,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Practice, UUID>

    fun findAllByMemberAndPaging(
        memberId: Long,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Practice, UUID>

    fun searchByMemberAndKeywordAndPaging(
        memberId: Long,
        keyword: String,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Practice, UUID>
}
