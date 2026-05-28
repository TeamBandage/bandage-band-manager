package com.bandage.v1.domain.setlist.repository

import com.bandage.v1.domain.setlist.model.QSetlistTrack
import com.bandage.v1.domain.setlist.model.SetlistTrack
import com.bandage.v1.global.common.response.CursorResponse
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import java.util.UUID

class SetlistTrackRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : SetlistTrackRepositoryCustom {
    override fun findAllBySetlistAndPaging(
        setlistId: UUID,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<SetlistTrack, UUID> {
        val qTrack = QSetlistTrack.setlistTrack

        val contents =
            queryFactory
                .selectFrom(qTrack)
                .where(qTrack.setlist.id.eq(setlistId))
                .where(gtTrackId(lastId))
                .orderBy(qTrack.id.asc())
                .limit(pageSize.toLong() + 1)
                .fetch()

        val hasNext = contents.size > pageSize
        val resultContents = if (hasNext) contents.dropLast(1) else contents
        val nextCursor = if (hasNext) resultContents.lastOrNull()?.id else null

        return CursorResponse(content = resultContents, nextCursor = nextCursor, hasNext = hasNext)
    }

    private fun gtTrackId(lastId: UUID?): BooleanExpression? = lastId?.let { QSetlistTrack.setlistTrack.id.gt(it) }
}
