package com.bandage.bandmanager.domain.notify.service

import com.bandage.bandmanager.domain.notify.model.Notification
import com.bandage.bandmanager.domain.notify.repository.NotificationRepository
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import com.bandage.bandmanager.global.notify.annotation.NotifyCategory
import com.bandage.bandmanager.global.notify.resolver.NotificationPayload
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyIterable
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.Optional
import java.util.UUID

class NotificationServiceTest {
    private val notificationRepository = mock(NotificationRepository::class.java)
    private val sut = NotificationService(notificationRepository)

    private fun notification(recipientId: Long): Notification {
        val notification =
            Notification.create(
                NotificationPayload(
                    recipientId = recipientId,
                    category = NotifyCategory.BAND_APPLICATION,
                    title = "제목",
                    message = "메시지",
                ),
            )
        val field = Notification::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(notification, UUID.randomUUID())
        return notification
    }

    @Test
    fun `본인 알림은 읽음 처리된다`() {
        val notification = notification(1L)
        `when`(notificationRepository.findById(notification.id)).thenReturn(Optional.of(notification))

        sut.markAsRead(1L, notification.id)

        assertThat(notification.isRead).isTrue()
        assertThat(notification.readAt).isNotNull()
    }

    @Test
    fun `타인 알림 읽음 처리 시 NOTIFICATION_FORBIDDEN`() {
        val notification = notification(1L)
        `when`(notificationRepository.findById(notification.id)).thenReturn(Optional.of(notification))

        assertThatThrownBy { sut.markAsRead(999L, notification.id) }
            .isInstanceOfSatisfying(BusinessException::class.java) {
                assertThat(it.errorCode).isEqualTo(ErrorCode.NOTIFICATION_FORBIDDEN)
            }
    }

    @Test
    fun `존재하지 않는 알림 읽음 처리 시 NOTIFICATION_NOT_FOUND`() {
        val id = UUID.randomUUID()
        `when`(notificationRepository.findById(id)).thenReturn(Optional.empty())

        assertThatThrownBy { sut.markAsRead(1L, id) }
            .isInstanceOfSatisfying(BusinessException::class.java) {
                assertThat(it.errorCode).isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND)
            }
    }

    @Test
    fun `빈 페이로드는 저장하지 않는다`() {
        sut.createAll(emptyList())

        verify(notificationRepository, never()).saveAll(anyIterable())
    }

    @Test
    fun `페이로드가 있으면 일괄 저장한다`() {
        sut.createAll(listOf(NotificationPayload(1L, NotifyCategory.BAND_APPLICATION, "제목", "메시지")))

        verify(notificationRepository).saveAll(anyIterable())
    }

    @Test
    fun `미확인 개수를 반환한다`() {
        `when`(notificationRepository.countByRecipientIdAndIsReadFalse(1L)).thenReturn(3L)

        assertThat(sut.getUnreadCount(1L)).isEqualTo(3L)
    }
}
