package com.bandage.bandmanager.domain.notify.event

import com.bandage.bandmanager.domain.notify.service.NotificationService
import com.bandage.bandmanager.global.notify.event.NotificationCreateEvent
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 비즈니스 트랜잭션 커밋 후(AFTER_COMMIT) 비동기로 알림을 저장한다.
 * - 비즈니스 롤백 시 호출되지 않아 알림이 생성되지 않는다.
 * - @Async 별도 스레드(트랜잭션 없음)이므로 createAll 이 자체 @Transactional 로 저장한다.
 */
@Component
class NotificationEventHandler(
    private val notificationService: NotificationService,
) {
    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: NotificationCreateEvent) {
        notificationService.createAll(event.payloads)
    }
}
