package com.bandage.bandmanager.domain.setlist.repository

import com.bandage.bandmanager.domain.setlist.model.QSetlist
import com.bandage.bandmanager.domain.setlist.model.QSetlistTrack
import com.bandage.bandmanager.domain.setlist.model.QSetlistTrackParticipant
import com.bandage.bandmanager.domain.setlist.model.Setlist
import com.bandage.bandmanager.global.common.response.CursorResponse
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import java.util.UUID

class SetlistRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : SetlistRepositoryCustom {
    override fun findAllByMemberAndPaging(
        memberId: Long,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Setlist, UUID> {
        val qSetlist = QSetlist.setlist
        val qTrack = QSetlistTrack.setlistTrack
        val qParticipant = QSetlistTrackParticipant.setlistTrackParticipant

        val participantSetlistIds =
            JPAExpressions
                .select(qTrack.setlist.id)
                .from(qParticipant)
                .join(qParticipant.track, qTrack)
                .where(qParticipant.memberId.eq(memberId))

        val contents =
            queryFactory
                .selectFrom(qSetlist)
                .where(
                    qSetlist.managerId
                        .eq(memberId)
                        .or(qSetlist.id.`in`(participantSetlistIds)),
                ).where(ltSetlistId(lastId))
                .orderBy(qSetlist.id.desc())
                .limit(pageSize.toLong() + 1)
                .fetch()

        val hasNext = contents.size > pageSize
        val resultContents = if (hasNext) contents.dropLast(1) else contents
        val nextCursor = if (hasNext) resultContents.lastOrNull()?.id else null

        return CursorResponse(content = resultContents, nextCursor = nextCursor, hasNext = hasNext)
    }

    override fun findAllAccessibleByTitle(
        title: String,
        memberId: Long,
    ): List<Setlist> {
        val qSetlist = QSetlist.setlist
        val qTrack = QSetlistTrack.setlistTrack
        val qParticipant = QSetlistTrackParticipant.setlistTrackParticipant

        val participantSetlistIds =
            JPAExpressions
                .select(qTrack.setlist.id)
                .from(qParticipant)
                .join(qParticipant.track, qTrack)
                .where(qParticipant.memberId.eq(memberId))

        return queryFactory
            .selectFrom(qSetlist)
            .where(qSetlist.title.eq(title))
            .where(
                qSetlist.managerId
                    .eq(memberId)
                    .or(qSetlist.id.`in`(participantSetlistIds)),
            ).orderBy(qSetlist.id.desc())
            .fetch()
    }

    override fun isAccessibleMember(
        setlistId: UUID,
        memberId: Long,
    ): Boolean {
        val qSetlist = QSetlist.setlist
        val qTrack = QSetlistTrack.setlistTrack
        val qParticipant = QSetlistTrackParticipant.setlistTrackParticipant

        val participantSetlistIds =
            JPAExpressions
                .select(qTrack.setlist.id)
                .from(qParticipant)
                .join(qParticipant.track, qTrack)
                .where(qParticipant.memberId.eq(memberId))

        return queryFactory
            .selectOne()
            .from(qSetlist)
            .where(qSetlist.id.eq(setlistId))
            .where(
                qSetlist.managerId
                    .eq(memberId)
                    .or(qSetlist.id.`in`(participantSetlistIds)),
            ).fetchFirst() != null
    }

    private fun ltSetlistId(lastId: UUID?): BooleanExpression? = lastId?.let { QSetlist.setlist.id.lt(it) }
}
