package com.bandage.bandmanager.domain.notify.repository

import com.bandage.bandmanager.domain.notify.model.Notification
import com.bandage.bandmanager.global.common.response.CursorResponse
import java.util.UUID

interface NotificationRepositoryCustom {
    fun findAllByRecipientIdWithCursor(
        recipientId: Long,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Notification, UUID>
}
