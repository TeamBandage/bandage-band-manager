package com.bandage.bandmanager.global.notify.resolver

import com.bandage.bandmanager.global.notify.annotation.NotifyCategory

/**
 * 알림 생성 단위. "누구에게(recipientId) 무엇을(title/message)".
 *
 * @property referenceId 연관 리소스(밴드/합주 등) id. 프론트 딥링크/멱등성 키로 사용.
 */
data class NotificationPayload(
    val recipientId: Long,
    val category: NotifyCategory,
    val title: String,
    val message: String,
    val referenceId: String? = null,
)
