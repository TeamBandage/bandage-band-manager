package com.bandage.bandmanager.domain.notify.dto.res

import com.bandage.bandmanager.domain.notify.model.Notification
import com.bandage.bandmanager.global.notify.annotation.NotifyCategory
import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "알림 응답")
data class NotificationResponse(
    @Schema(description = "알림 고유 식별자 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val id: UUID,
    @Schema(description = "알림 카테고리 (프론트 필터 키)", example = "BAND_APPLICATION")
    val category: NotifyCategory,
    @Schema(description = "알림 제목", example = "새로운 가입 신청")
    val title: String,
    @Schema(description = "알림 메시지", example = "밴드에 새로운 가입 신청이 접수되었습니다.")
    val message: String,
    @Schema(description = "연관 리소스 ID (밴드/합주 등, 딥링크용)", example = "550e8400-e29b-41d4-a716-446655440000")
    val referenceId: String?,
    @Schema(description = "읽음 여부", example = "false")
    val isRead: Boolean,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", shape = JsonFormat.Shape.STRING, timezone = "Asia/Seoul")
    @Schema(description = "읽은 시각", example = "2026-06-20 18:00")
    val readAt: LocalDateTime?,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", shape = JsonFormat.Shape.STRING, timezone = "Asia/Seoul")
    @Schema(description = "생성 시각", example = "2026-06-20 17:30")
    val createdAt: LocalDateTime,
) {
    companion object {
        fun of(notification: Notification): NotificationResponse =
            NotificationResponse(
                id = notification.id,
                category = notification.category,
                title = notification.title,
                message = notification.message,
                referenceId = notification.referenceId,
                isRead = notification.isRead,
                readAt = notification.readAt,
                createdAt = notification.createdAt,
            )
    }
}
