package com.bandage.bandmanager.domain.member.repository

import com.bandage.bandmanager.domain.member.model.Member
import com.bandage.bandmanager.domain.member.model.QMember
import com.bandage.bandmanager.global.common.response.CursorResponse
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory

class MemberRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : MemberRepositoryCustom {
    override fun searchByKeywordAndPaging(
        keyword: String,
        excludeMemberId: Long?,
        lastId: Long?,
        pageSize: Int,
    ): CursorResponse<Member, Long> {
        val qMember = QMember.member

        val rows =
            queryFactory
                .selectFrom(qMember)
                .where(
                    qMember.name
                        .containsIgnoreCase(keyword)
                        .or(qMember.email.containsIgnoreCase(keyword)),
                ).where(neMemberId(excludeMemberId))
                .where(ltMemberId(lastId))
                .orderBy(qMember.id.desc())
                .limit(pageSize.toLong() + 1)
                .fetch()

        return CursorResponse.of(rows, pageSize) { it.id }
    }

    private fun neMemberId(memberId: Long?): BooleanExpression? = memberId?.let { QMember.member.id.ne(it) }

    private fun ltMemberId(lastId: Long?): BooleanExpression? = lastId?.let { QMember.member.id.lt(it) }
}
