package com.bandage.bandmanager.domain.selection.repository

import com.bandage.bandmanager.domain.selection.model.QTrackSelectionItemChatMessage
import com.bandage.bandmanager.domain.selection.model.TrackSelectionItemChatMessage
import com.bandage.bandmanager.global.common.response.CursorResponse
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import java.util.UUID

class TrackSelectionItemChatMessageRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : TrackSelectionItemChatMessageRepositoryCustom {
    override fun findAllByItemAndPaging(
        itemId: UUID,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<TrackSelectionItemChatMessage, UUID> {
        val qChat = QTrackSelectionItemChatMessage.trackSelectionItemChatMessage

        val contents =
            queryFactory
                .selectFrom(qChat)
                .where(qChat.item.id.eq(itemId))
                .where(ltChatId(lastId))
                .orderBy(qChat.id.desc())
                .limit(pageSize.toLong() + 1)
                .fetch()

        val hasNext = contents.size > pageSize
        val resultContents = if (hasNext) contents.dropLast(1) else contents
        val nextCursor = if (hasNext) resultContents.lastOrNull()?.id else null

        return CursorResponse(content = resultContents, nextCursor = nextCursor, hasNext = hasNext)
    }

    private fun ltChatId(lastId: UUID?): BooleanExpression? =
        lastId?.let { QTrackSelectionItemChatMessage.trackSelectionItemChatMessage.id.lt(it) }
}
