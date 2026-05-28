package com.bandage.v1.domain.selection.repository

import com.bandage.v1.domain.selection.model.QTrackSelection
import com.bandage.v1.domain.selection.model.QTrackSelectionMember
import com.bandage.v1.domain.selection.model.TrackSelection
import com.bandage.v1.global.common.response.CursorResponse
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import java.util.UUID

class TrackSelectionRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : TrackSelectionRepositoryCustom {
    override fun findAllByMemberAndPaging(
        memberId: Long,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<TrackSelection, UUID> {
        val qSelection = QTrackSelection.trackSelection
        val qMember = QTrackSelectionMember.trackSelectionMember

        val contents =
            queryFactory
                .selectFrom(qSelection)
                .join(qMember)
                .on(qMember.selection.eq(qSelection))
                .where(qMember.memberId.eq(memberId))
                .where(ltSelectionId(lastId))
                .orderBy(qSelection.id.desc())
                .distinct()
                .limit(pageSize.toLong() + 1)
                .fetch()

        val hasNext = contents.size > pageSize
        val resultContents = if (hasNext) contents.dropLast(1) else contents
        val nextCursor = if (hasNext) resultContents.lastOrNull()?.id else null

        return CursorResponse(content = resultContents, nextCursor = nextCursor, hasNext = hasNext)
    }

    private fun ltSelectionId(lastId: UUID?): BooleanExpression? = lastId?.let { QTrackSelection.trackSelection.id.lt(it) }
}
