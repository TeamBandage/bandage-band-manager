package com.bandage.bandmanager.domain.notify.service

import com.bandage.bandmanager.domain.notify.dto.req.NotificationPagingQuery
import com.bandage.bandmanager.domain.notify.dto.res.NotificationResponse
import com.bandage.bandmanager.domain.notify.model.Notification
import com.bandage.bandmanager.domain.notify.repository.NotificationRepository
import com.bandage.bandmanager.global.common.response.CursorResponse
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import com.bandage.bandmanager.global.notify.resolver.NotificationPayload
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional(readOnly = true)
class NotificationService(
    private val notificationRepository: NotificationRepository,
) {
    /**
     * 이벤트 핸들러(@Async, AFTER_COMMIT)에서 호출. 비동기 스레드에는 트랜잭션이 없으므로
     * 자체 @Transactional 로 저장한다. 대량 수신자는 saveAll 배치 insert.
     */
    @Transactional
    fun createAll(payloads: List<NotificationPayload>) {
        if (payloads.isEmpty()) return
        notificationRepository.saveAll(payloads.map { Notification.create(it) })
    }

    fun getMyNotifications(
        memberId: Long,
        query: NotificationPagingQuery,
    ): CursorResponse<NotificationResponse, UUID> {
        val result =
            notificationRepository.findAllByRecipientIdWithCursor(memberId, query.lastId, query.pageSize, query.unreadOnly)
        return CursorResponse(
            content = result.content.map { NotificationResponse.of(it) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    fun getUnreadCount(memberId: Long): Long = notificationRepository.countByRecipientIdAndIsReadFalse(memberId)

    @Transactional
    fun markAsRead(
        memberId: Long,
        notificationId: UUID,
    ) {
        val notification =
            notificationRepository.findByIdOrNull(notificationId)
                ?: throw BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND)
        if (notification.recipientId != memberId) {
            throw BusinessException(ErrorCode.NOTIFICATION_FORBIDDEN)
        }
        notification.markAsRead()
    }

    @Transactional
    fun markAllAsRead(memberId: Long): Int = notificationRepository.markAllAsReadByRecipient(memberId, LocalDateTime.now())
}
