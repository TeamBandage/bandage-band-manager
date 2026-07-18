package com.bandage.bandmanager.domain.notify.repository

import com.bandage.bandmanager.domain.notify.model.Notification
import com.bandage.bandmanager.domain.notify.model.QNotification
import com.bandage.bandmanager.global.common.response.CursorResponse
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import java.util.UUID

class NotificationRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : NotificationRepositoryCustom {
    override fun findAllByRecipientIdWithCursor(
        recipientId: Long,
        lastId: UUID?,
        pageSize: Int,
        unreadOnly: Boolean,
    ): CursorResponse<Notification, UUID> {
        val qNotification = QNotification.notification

        val contents =
            queryFactory
                .selectFrom(qNotification)
                .where(qNotification.recipientId.eq(recipientId))
                .where(ltNotificationId(lastId))
                .where(unreadOnlyCondition(unreadOnly))
                .orderBy(qNotification.id.desc())
                .limit(pageSize.toLong() + 1)
                .fetch()

        val hasNext = contents.size > pageSize
        val resultContents = if (hasNext) contents.dropLast(1) else contents
        val nextCursor = if (hasNext) resultContents.lastOrNull()?.id else null

        return CursorResponse(
            content = resultContents,
            nextCursor = nextCursor,
            hasNext = hasNext,
        )
    }

    private fun ltNotificationId(lastId: UUID?): BooleanExpression? = lastId?.let { QNotification.notification.id.lt(it) }

    private fun unreadOnlyCondition(unreadOnly: Boolean): BooleanExpression? =
        if (unreadOnly) QNotification.notification.isRead.isFalse else null
}
