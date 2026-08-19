package com.bandage.bandmanager.domain.member.repository

import com.bandage.bandmanager.domain.member.model.Member
import com.bandage.bandmanager.global.common.response.CursorResponse

interface MemberRepositoryCustom {
    fun searchByKeywordAndPaging(
        keyword: String,
        excludeMemberId: Long?,
        lastId: Long?,
        pageSize: Int,
    ): CursorResponse<Member, Long>
}
