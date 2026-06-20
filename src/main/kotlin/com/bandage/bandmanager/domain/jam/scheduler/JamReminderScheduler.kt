package com.bandage.bandmanager.domain.jam.scheduler

import com.bandage.bandmanager.domain.jam.repository.JamRepository
import com.bandage.bandmanager.domain.notify.repository.NotificationRepository
import com.bandage.bandmanager.global.async.publisher.EventPublisher
import com.bandage.bandmanager.global.notify.annotation.NotifyCategory
import com.bandage.bandmanager.global.notify.event.NotificationCreateEvent
import com.bandage.bandmanager.global.notify.resolver.NotificationPayload
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 임박한 합주(시작 [REMIND_BEFORE_HOURS, +1h) 구간) 참여자에게 JAM_UPCOMING 알림을 생성한다.
 *
 * - AOP 경로가 아니므로 NotificationCreateEvent 를 직접 발행해 동일 저장 파이프라인을 재사용한다.
 * - 멱등성: (recipientId, JAM_UPCOMING, jamId) 가 이미 있으면 제외하여 중복/재실행에도 1회만 생성.
 * - @Transactional(readOnly): jam.participants(LAZY) 로딩 + 커밋 시 AFTER_COMMIT 핸들러 발화.
 */
@Component
class JamReminderScheduler(
    private val jamRepository: JamRepository,
    private val notificationRepository: NotificationRepository,
    private val eventPublisher: EventPublisher,
) {
    @Scheduled(cron = "0 0 * * * *")
    @Transactional(readOnly = true)
    fun remindUpcomingJams() {
        val from = LocalDateTime.now().plusHours(REMIND_BEFORE_HOURS)
        val to = from.plusHours(1)

        val payloads =
            jamRepository.findUpcomingBetween(from, to).flatMap { jam ->
                jam.participants
                    .map { it.member }
                    .distinct()
                    .filter { memberId ->
                        !notificationRepository.existsByRecipientIdAndCategoryAndReferenceId(
                            memberId,
                            NotifyCategory.JAM_UPCOMING,
                            jam.id.toString(),
                        )
                    }.map { memberId ->
                        NotificationPayload(
                            recipientId = memberId,
                            category = NotifyCategory.JAM_UPCOMING,
                            title = "다가오는 합주",
                            message = "'${jam.title}' 합주가 곧 시작됩니다.",
                            referenceId = jam.id.toString(),
                        )
                    }
            }

        if (payloads.isNotEmpty()) {
            eventPublisher.publish(NotificationCreateEvent(payloads))
        }
    }

    companion object {
        const val REMIND_BEFORE_HOURS = 24L
    }
}
