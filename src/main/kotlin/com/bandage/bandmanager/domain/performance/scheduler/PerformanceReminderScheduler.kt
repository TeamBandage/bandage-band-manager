package com.bandage.bandmanager.domain.performance.scheduler

import com.bandage.bandmanager.domain.notify.repository.NotificationRepository
import com.bandage.bandmanager.domain.performance.repository.PerformanceRepository
import com.bandage.bandmanager.global.async.publisher.EventPublisher
import com.bandage.bandmanager.global.notify.annotation.NotifyCategory
import com.bandage.bandmanager.global.notify.event.NotificationCreateEvent
import com.bandage.bandmanager.global.notify.resolver.NotificationPayload
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 임박한 공연(시작 [REMIND_BEFORE_HOURS, +1h) 구간) 참여자(매니저 전원)에게 PERFORMANCE_UPCOMING 알림을 생성한다.
 *
 * - JamReminderScheduler 와 동일한 구조: AOP 경로가 아니므로 NotificationCreateEvent 를 직접 발행.
 * - 멱등성: (recipientId, PERFORMANCE_UPCOMING, performanceId) 가 이미 있으면 제외해 재실행에도 1회만 생성.
 * - @Transactional(readOnly): performance.managers(LAZY) 로딩 + 커밋 시 AFTER_COMMIT 핸들러 발화.
 */
@Component
class PerformanceReminderScheduler(
    private val performanceRepository: PerformanceRepository,
    private val notificationRepository: NotificationRepository,
    private val eventPublisher: EventPublisher,
) {
    @Scheduled(cron = "0 0 * * * *")
    @Transactional(readOnly = true)
    fun remindUpcomingPerformances() {
        val from = LocalDateTime.now().plusHours(REMIND_BEFORE_HOURS)
        val to = from.plusHours(1)

        val payloads =
            performanceRepository.findUpcomingBetween(from, to).flatMap { performance ->
                performance.managers
                    .map { it.member }
                    .distinct()
                    .filter { memberId ->
                        !notificationRepository.existsByRecipientIdAndCategoryAndReferenceId(
                            memberId,
                            NotifyCategory.PERFORMANCE_UPCOMING,
                            performance.id.toString(),
                        )
                    }.map { memberId ->
                        NotificationPayload(
                            recipientId = memberId,
                            category = NotifyCategory.PERFORMANCE_UPCOMING,
                            title = "다가오는 공연",
                            message = "'${performance.title}' 공연이 곧 시작됩니다.",
                            referenceId = performance.id.toString(),
                        )
                    }
            }

        if (payloads.isNotEmpty()) {
            eventPublisher.publish(NotificationCreateEvent(payloads))
        }
    }

    companion object {
        // 명세: 1일 전 기준
        const val REMIND_BEFORE_HOURS = 24L
    }
}
