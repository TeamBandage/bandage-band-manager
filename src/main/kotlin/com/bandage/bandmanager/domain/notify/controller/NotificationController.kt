package com.bandage.bandmanager.domain.notify.controller

import com.bandage.bandmanager.domain.notify.dto.req.NotificationPagingQuery
import com.bandage.bandmanager.domain.notify.dto.res.NotificationResponse
import com.bandage.bandmanager.domain.notify.dto.res.UnreadCountResponse
import com.bandage.bandmanager.domain.notify.service.NotificationService
import com.bandage.bandmanager.global.common.constants.PathPrefix.PREFIX
import com.bandage.bandmanager.global.common.response.ApiResponse
import com.bandage.bandmanager.global.common.response.CursorResponse
import com.bandage.bandmanager.global.security.annotation.CurrentMemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "notifications", description = "알림 API")
@RestController
@RequestMapping("$PREFIX/notifications")
class NotificationController(
    private val notificationService: NotificationService,
) {
    @GetMapping
    @Operation(
        operationId = "getMyNotifications",
        summary = "내 알림 목록 조회 API",
        description = "로그인한 회원의 전체 알림을 커서 기반 최신순으로 조회합니다. 카테고리 필터링은 프론트가 처리합니다.",
    )
    fun getMyNotifications(
        @Valid query: NotificationPagingQuery,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<CursorResponse<NotificationResponse, UUID>> =
        ApiResponse.success(notificationService.getMyNotifications(memberId, query))

    @GetMapping("/unread-count")
    @Operation(
        operationId = "getUnreadNotificationCount",
        summary = "미확인 알림 개수 조회 API",
        description = "로그인한 회원의 읽지 않은 알림 개수를 조회합니다.",
    )
    fun getUnreadCount(
        @CurrentMemberId memberId: Long,
    ): ApiResponse<UnreadCountResponse> = ApiResponse.success(UnreadCountResponse(notificationService.getUnreadCount(memberId)))

    @PatchMapping("/{notificationId}/read")
    @Operation(
        operationId = "markNotificationAsRead",
        summary = "알림 읽음 처리 API",
        description = "특정 알림을 읽음 처리합니다. 본인 소유 알림만 처리할 수 있습니다.",
    )
    fun markAsRead(
        @PathVariable notificationId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        notificationService.markAsRead(memberId, notificationId)
        return ApiResponse.success()
    }

    @PatchMapping("/read-all")
    @Operation(
        operationId = "markAllNotificationsAsRead",
        summary = "전체 알림 읽음 처리 API",
        description = "로그인한 회원의 읽지 않은 모든 알림을 일괄 읽음 처리합니다.",
    )
    fun markAllAsRead(
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        notificationService.markAllAsRead(memberId)
        return ApiResponse.success()
    }
}
