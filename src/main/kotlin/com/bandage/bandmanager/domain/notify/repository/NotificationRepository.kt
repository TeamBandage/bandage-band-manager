package com.bandage.bandmanager.domain.notify.repository

import com.bandage.bandmanager.domain.notify.model.Notification
import com.bandage.bandmanager.global.notify.annotation.NotifyCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface NotificationRepository :
    JpaRepository<Notification, UUID>,
    NotificationRepositoryCustom {
    fun countByRecipientIdAndIsReadFalse(recipientId: Long): Long

    // 스케줄러 멱등성: 동일 (수신자, 카테고리, 리소스) 알림 존재 여부
    fun existsByRecipientIdAndCategoryAndReferenceId(
        recipientId: Long,
        category: NotifyCategory,
        referenceId: String,
    ): Boolean

    @Modifying(clearAutomatically = true)
    @Query(
        "UPDATE Notification n SET n.isRead = true, n.readAt = :now " +
            "WHERE n.recipientId = :recipientId AND n.isRead = false AND n.deletedAt IS NULL",
    )
    fun markAllAsReadByRecipient(
        @Param("recipientId") recipientId: Long,
        @Param("now") now: LocalDateTime,
    ): Int
}
