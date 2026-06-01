package com.bandage.v1.domain.jam.repository

import com.bandage.v1.domain.jam.model.Jam
import com.bandage.v1.domain.jam.model.QJam
import com.bandage.v1.domain.jam.model.QJamParticipant
import com.bandage.v1.global.common.response.CursorResponse
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import java.util.UUID

class JamRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : JamRepositoryCustom {
    override fun findAllByPaging(
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Jam, UUID> {
        val qJam = QJam.jam

        val contents =
            queryFactory
                .selectFrom(qJam)
                .where(ltJamId(lastId))
                .orderBy(qJam.id.desc())
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
    ): CursorResponse<Jam, UUID> {
        val qJam = QJam.jam
        val qParticipant = QJamParticipant.jamParticipant

        val contents =
            queryFactory
                .selectFrom(qJam)
                .join(qParticipant)
                .on(qParticipant.jam.eq(qJam))
                .where(qParticipant.member.`in`(memberIds))
                .where(ltJamId(lastId))
                .orderBy(qJam.id.desc())
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
    ): CursorResponse<Jam, UUID> {
        val qJam = QJam.jam
        val qParticipant = QJamParticipant.jamParticipant

        val contents =
            queryFactory
                .selectFrom(qJam)
                .join(qParticipant)
                .on(qParticipant.jam.eq(qJam))
                .where(qParticipant.member.eq(memberId))
                .where(ltJamId(lastId))
                .orderBy(qJam.id.desc())
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
    ): CursorResponse<Jam, UUID> {
        val qJam = QJam.jam
        val qParticipant = QJamParticipant.jamParticipant

        val contents =
            queryFactory
                .selectFrom(qJam)
                .join(qParticipant)
                .on(qParticipant.jam.eq(qJam))
                .where(qParticipant.member.eq(memberId))
                .where(qJam.title.containsIgnoreCase(keyword).or(qJam.trackInfo.title.containsIgnoreCase(keyword)))
                .where(ltJamId(lastId))
                .orderBy(qJam.id.desc())
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

    private fun ltJamId(lastId: UUID?): BooleanExpression? = lastId?.let { QJam.jam.id.lt(it) }
}
