package com.bandage.bandmanager.domain.selection.repository

import com.bandage.bandmanager.domain.selection.model.QTrackSelectionItem
import com.bandage.bandmanager.domain.selection.model.TrackSelectionItem
import com.bandage.bandmanager.global.common.response.CursorResponse
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import java.util.UUID

class TrackSelectionItemRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : TrackSelectionItemRepositoryCustom {
    override fun findAllBySelectionAndPaging(
        selectionId: UUID,
        memberId: Long,
        filter: TrackSelectionItemFilter,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<TrackSelectionItem, UUID> {
        val qItem = QTrackSelectionItem.trackSelectionItem

        // 필터 술어는 모두 EXISTS/스칼라 조건이라 outer FROM 이 단일 테이블로 유지된다.
        // 따라서 항목당 1행이 보장되어 limit(pageSize+1)/dropLast(1) 커서 계산이 그대로 유효하다.
        val contents =
            queryFactory
                .selectFrom(qItem)
                .where(qItem.selection.id.eq(selectionId))
                .where(ltItemId(lastId))
                .where(TrackSelectionItemFilterPredicates.statusIn(filter.status))
                .where(TrackSelectionItemFilterPredicates.appliedByMe(filter.appliedByMe, memberId))
                .where(TrackSelectionItemFilterPredicates.memberNameContains(filter.memberName))
                .where(TrackSelectionItemFilterPredicates.trackInfoContains(filter.keyword, filter.searchFields))
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
