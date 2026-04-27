package com.bandage.v1.domain.setlist.repository

import com.bandage.v1.domain.setlist.model.QSetlistItemChatMessage
import com.bandage.v1.domain.setlist.model.SetlistItemChatMessage
import com.bandage.v1.global.common.response.CursorResponse
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import java.util.UUID

class SetlistItemChatMessageRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : SetlistItemChatMessageRepositoryCustom {
    override fun findAllByItemAndPaging(
        itemId: UUID,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<SetlistItemChatMessage, UUID> {
        val qChat = QSetlistItemChatMessage.setlistItemChatMessage

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

    private fun ltChatId(lastId: UUID?): BooleanExpression? = lastId?.let { QSetlistItemChatMessage.setlistItemChatMessage.id.lt(it) }
}
