package com.bandage.bandmanager.global.notify.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.transaction.annotation.EnableTransactionManagement

/**
 * 알림 도메인 비동기/스케줄링/트랜잭션 순서 설정.
 *
 * - `@EnableTransactionManagement(order = 0)`: 트랜잭션 어드바이스를 가장 바깥쪽(우선순위 최상)에 둔다.
 *   NotifyAspect(@Order(100)) 가 트랜잭션보다 안쪽에서 실행되어, 이벤트 발행이 활성 트랜잭션 내부에서
 *   일어나도록 보장한다. 이 순서가 어긋나면 @TransactionalEventListener(AFTER_COMMIT) 가 호출되지 않는다.
 * - `notificationExecutor`: AFTER_COMMIT 핸들러의 @Async 전용 풀.
 */
@Configuration
@EnableScheduling
@EnableTransactionManagement(order = 0)
class NotifyAsyncConfig {
    @Bean("notificationExecutor")
    fun notificationExecutor(): TaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = 2
            maxPoolSize = 5
            queueCapacity = 500
            setThreadNamePrefix("notify-")
            setWaitForTasksToCompleteOnShutdown(true)
            initialize()
        }
}
