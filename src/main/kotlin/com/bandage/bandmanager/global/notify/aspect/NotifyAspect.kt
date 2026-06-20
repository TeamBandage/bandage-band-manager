package com.bandage.bandmanager.global.notify.aspect

import com.bandage.bandmanager.global.async.publisher.EventPublisher
import com.bandage.bandmanager.global.notify.annotation.Notify
import com.bandage.bandmanager.global.notify.event.NotificationCreateEvent
import com.bandage.bandmanager.global.notify.resolver.support.NotifyResolverRegistry
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * @Notify 가 붙은 메서드가 정상 반환하면, category 의 Resolver 로 수신자/내용을 추출해
 * NotificationCreateEvent 를 발행한다.
 *
 * ⚠️ 순서 제약: 트랜잭션 어드바이스(@EnableTransactionManagement(order = 0))보다 안쪽(ORDER = 100)이어야
 * 발행이 활성 트랜잭션 내부에서 일어나, AFTER_COMMIT 핸들러가 동작한다. (NotifyAsyncConfig 참고)
 */
@Aspect
@Component
@Order(NotifyAspect.ORDER)
class NotifyAspect(
    private val registry: NotifyResolverRegistry,
    private val eventPublisher: EventPublisher,
) {
    @AfterReturning(
        pointcut = "@annotation(notify)",
        returning = "returnValue",
    )
    fun afterReturning(
        joinPoint: JoinPoint,
        notify: Notify,
        returnValue: Any?,
    ) {
        val payloads = registry.resolve(notify.category, joinPoint.args, returnValue)
        if (payloads.isNotEmpty()) {
            eventPublisher.publish(NotificationCreateEvent(payloads))
        }
    }

    companion object {
        const val ORDER = 100
    }
}
