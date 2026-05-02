package com.bandage.v1.domain.setlist.repository

import com.bandage.v1.domain.setlist.model.QSetlistItem
import com.bandage.v1.domain.setlist.model.SetlistItem
import com.bandage.v1.global.common.response.CursorResponse
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import java.util.UUID

class SetlistItemRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : SetlistItemRepositoryCustom {
    override fun findAllByMeetingAndPaging(
        meetingId: UUID,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<SetlistItem, UUID> {
        val qItem = QSetlistItem.setlistItem

        val contents =
            queryFactory
                .selectFrom(qItem)
                .where(qItem.meeting.id.eq(meetingId))
                .where(ltItemId(lastId))
                .orderBy(qItem.id.asc())
                .limit(pageSize.toLong() + 1)
                .fetch()

        val hasNext = contents.size > pageSize
        val resultContents = if (hasNext) contents.dropLast(1) else contents
        val nextCursor = if (hasNext) resultContents.lastOrNull()?.id else null

        return CursorResponse(content = resultContents, nextCursor = nextCursor, hasNext = hasNext)
    }

    private fun ltItemId(lastId: UUID?): BooleanExpression? = lastId?.let { QSetlistItem.setlistItem.id.gt(it) }
}
