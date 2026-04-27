package com.bandage.v1.domain.setlist.repository

import com.bandage.v1.domain.setlist.model.QSetlistMeeting
import com.bandage.v1.domain.setlist.model.QSetlistMeetingMember
import com.bandage.v1.domain.setlist.model.SetlistMeeting
import com.bandage.v1.global.common.response.CursorResponse
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import java.util.UUID

class SetlistMeetingRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : SetlistMeetingRepositoryCustom {
    override fun findAllByMemberAndPaging(
        memberId: Long,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<SetlistMeeting, UUID> {
        val qMeeting = QSetlistMeeting.setlistMeeting
        val qMember = QSetlistMeetingMember.setlistMeetingMember

        val contents =
            queryFactory
                .selectFrom(qMeeting)
                .join(qMember)
                .on(qMember.meeting.eq(qMeeting))
                .where(qMember.memberId.eq(memberId))
                .where(ltMeetingId(lastId))
                .orderBy(qMeeting.id.desc())
                .distinct()
                .limit(pageSize.toLong() + 1)
                .fetch()

        val hasNext = contents.size > pageSize
        val resultContents = if (hasNext) contents.dropLast(1) else contents
        val nextCursor = if (hasNext) resultContents.lastOrNull()?.id else null

        return CursorResponse(content = resultContents, nextCursor = nextCursor, hasNext = hasNext)
    }

    private fun ltMeetingId(lastId: UUID?): BooleanExpression? = lastId?.let { QSetlistMeeting.setlistMeeting.id.lt(it) }
}
