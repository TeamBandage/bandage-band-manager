package com.bandage.v1.domain.practice.repository

import com.bandage.v1.domain.practice.model.Practice
import com.bandage.v1.domain.practice.model.QPractice
import com.bandage.v1.domain.practice.model.QPracticeParticipant
import com.bandage.v1.global.common.response.CursorResponse
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import java.util.UUID

class PracticeRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : PracticeRepositoryCustom {
    override fun findAllByPaging(
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Practice, UUID> {
        val qPractice = QPractice.practice

        val contents =
            queryFactory
                .selectFrom(qPractice)
                .where(ltPracticeId(lastId))
                .orderBy(qPractice.id.desc())
                .limit(pageSize.toLong() + 1)
                .fetch()

        val hasNext = contents.size > pageSize
        val resultContents = if (hasNext) contents.dropLast(1) else contents
        val nextCursor = if (hasNext) resultContents.lastOrNull()?.id else null

        return CursorResponse(
            content = resultContents,
            nextCursor = nextCursor,
            hasNext = hasNext,
        )
    }

    override fun findAllByMembersAndPaging(
        memberIds: List<Long>,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Practice, UUID> {
        val qPractice = QPractice.practice
        val qParticipant = QPracticeParticipant.practiceParticipant

        val contents =
            queryFactory
                .selectFrom(qPractice)
                .join(qParticipant)
                .on(qParticipant.practice.eq(qPractice))
                .where(qParticipant.member.`in`(memberIds))
                .where(ltPracticeId(lastId))
                .orderBy(qPractice.id.desc())
                .distinct()
                .limit(pageSize.toLong() + 1)
                .fetch()

        val hasNext = contents.size > pageSize
        val resultContents = if (hasNext) contents.dropLast(1) else contents
        val nextCursor = if (hasNext) resultContents.lastOrNull()?.id else null

        return CursorResponse(
            content = resultContents,
            nextCursor = nextCursor,
            hasNext = hasNext,
        )
    }

    override fun findAllByMemberAndPaging(
        memberId: Long,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Practice, UUID> {
        val qPractice = QPractice.practice
        val qParticipant = QPracticeParticipant.practiceParticipant

        val contents =
            queryFactory
                .selectFrom(qPractice)
                .join(qParticipant)
                .on(qParticipant.practice.eq(qPractice))
                .where(qParticipant.member.eq(memberId))
                .where(ltPracticeId(lastId))
                .orderBy(qPractice.id.desc())
                .distinct()
                .limit(pageSize.toLong() + 1)
                .fetch()

        val hasNext = contents.size > pageSize
        val resultContents = if (hasNext) contents.dropLast(1) else contents
        val nextCursor = if (hasNext) resultContents.lastOrNull()?.id else null

        return CursorResponse(
            content = resultContents,
            nextCursor = nextCursor,
            hasNext = hasNext,
        )
    }

    override fun searchByMemberAndKeywordAndPaging(
        memberId: Long,
        keyword: String,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Practice, UUID> {
        val qPractice = QPractice.practice
        val qParticipant = QPracticeParticipant.practiceParticipant

        val contents =
            queryFactory
                .selectFrom(qPractice)
                .join(qParticipant)
                .on(qParticipant.practice.eq(qPractice))
                .where(qParticipant.member.eq(memberId))
                .where(qPractice.title.containsIgnoreCase(keyword).or(qPractice.trackInfo.title.containsIgnoreCase(keyword)))
                .where(ltPracticeId(lastId))
                .orderBy(qPractice.id.desc())
                .distinct()
                .limit(pageSize.toLong() + 1)
                .fetch()

        val hasNext = contents.size > pageSize
        val resultContents = if (hasNext) contents.dropLast(1) else contents
        val nextCursor = if (hasNext) resultContents.lastOrNull()?.id else null

        return CursorResponse(
            content = resultContents,
            nextCursor = nextCursor,
            hasNext = hasNext,
        )
    }

    private fun ltPracticeId(lastId: UUID?): BooleanExpression? = lastId?.let { QPractice.practice.id.lt(it) }
}
