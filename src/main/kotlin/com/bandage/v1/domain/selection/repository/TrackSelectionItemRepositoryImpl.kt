package com.bandage.v1.domain.selection.repository

import com.bandage.v1.domain.selection.model.QTrackSelectionItem
import com.bandage.v1.domain.selection.model.TrackSelectionItem
import com.bandage.v1.global.common.response.CursorResponse
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import java.util.UUID

class TrackSelectionItemRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : TrackSelectionItemRepositoryCustom {
    override fun findAllBySelectionAndPaging(
        selectionId: UUID,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<TrackSelectionItem, UUID> {
        val qItem = QTrackSelectionItem.trackSelectionItem

        val contents =
            queryFactory
                .selectFrom(qItem)
                .where(qItem.selection.id.eq(selectionId))
                .where(ltItemId(lastId))
                .orderBy(qItem.id.asc())
                .limit(pageSize.toLong() + 1)
                .fetch()

        val hasNext = contents.size > pageSize
        val resultContents = if (hasNext) contents.dropLast(1) else contents
        val nextCursor = if (hasNext) resultContents.lastOrNull()?.id else null

        return CursorResponse(content = resultContents, nextCursor = nextCursor, hasNext = hasNext)
    }

    private fun ltItemId(lastId: UUID?): BooleanExpression? = lastId?.let { QTrackSelectionItem.trackSelectionItem.id.gt(it) }
}
