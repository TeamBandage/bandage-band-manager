package com.bandage.bandmanager.domain.notify.model

import com.bandage.bandmanager.global.common.domain.BaseEntity
import com.bandage.bandmanager.global.notify.annotation.NotifyCategory
import com.bandage.bandmanager.global.notify.resolver.NotificationPayload
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.UuidGenerator
import java.time.LocalDateTime
import java.util.UUID

/**
 * 수신자별 1행(한 사건이 N명에게 가면 N개 row) 모델. 개인별 읽음 상태 관리를 단순화한다.
 */
@Entity
@Table(
    name = "p_notification",
    indexes = [
        Index(name = "idx_notification__recipient", columnList = "recipient_id, created_at"),
        Index(name = "idx_notification__idempotency", columnList = "recipient_id, category, reference_id"),
    ],
)
@SQLRestriction("deleted_at IS NULL")
open class Notification(
    recipientId: Long,
    category: NotifyCategory,
    title: String,
    message: String,
    referenceId: String? = null,
) : BaseEntity() {
    @Id
    @Column(name = "notification_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @Column(name = "recipient_id", nullable = false)
    val recipientId: Long = recipientId

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    val category: NotifyCategory = category

    @Column(name = "title", nullable = false)
    val title: String = title

    @Column(name = "message", nullable = false, length = 500)
    val message: String = message

    // 연관 리소스(밴드/합주) id. 프론트 딥링크/멱등성 키.
    @Column(name = "reference_id")
    val referenceId: String? = referenceId

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false
        protected set

    @Column(name = "read_at")
    var readAt: LocalDateTime? = null
        protected set

    companion object {
        fun create(payload: NotificationPayload): Notification =
            Notification(
                recipientId = payload.recipientId,
                category = payload.category,
                title = payload.title,
                message = payload.message,
                referenceId = payload.referenceId,
            )
    }

    fun markAsRead() {
        if (!isRead) {
            isRead = true
            readAt = LocalDateTime.now()
        }
    }
}
