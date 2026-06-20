package com.bandage.bandmanager.global.notify.event

import com.bandage.bandmanager.global.async.event.CommonEvent
import com.bandage.bandmanager.global.async.event.EventType
import com.bandage.bandmanager.global.notify.resolver.NotificationPayload

/**
 * 알림 생성 요청 이벤트. AOP 트리거(NotifyAspect)와 스케줄러가 공유하는 수렴점.
 * AFTER_COMMIT 핸들러가 수신해 저장한다.
 *
 * Modulith 경계상 트리거(global)가 발행하므로 global 에 둔다(domain 은 이를 구독만).
 */
class NotificationCreateEvent(
    val payloads: List<NotificationPayload>,
) : CommonEvent(
        eventType = EventType.NOTIFICATION_CREATE,
        aggregateId = payloads.firstOrNull()?.recipientId?.toString() ?: "0",
    )
