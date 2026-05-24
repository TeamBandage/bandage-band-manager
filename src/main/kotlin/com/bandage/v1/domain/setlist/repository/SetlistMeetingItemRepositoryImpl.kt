package com.bandage.v1.domain.setlist.repository

import com.bandage.v1.domain.setlist.model.QSetlistMeetingItem
import com.bandage.v1.domain.setlist.model.SetlistMeetingItem
import com.bandage.v1.global.common.response.CursorResponse
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import java.util.UUID

class SetlistMeetingItemRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : SetlistMeetingItemRepositoryCustom {
    override fun findAllByMeetingAndPaging(
        meetingId: UUID,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<SetlistMeetingItem, UUID> {
        val qItem = QSetlistMeetingItem.setlistMeetingItem

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

    private fun ltItemId(lastId: UUID?): BooleanExpression? = lastId?.let { QSetlistMeetingItem.setlistMeetingItem.id.gt(it) }
}
