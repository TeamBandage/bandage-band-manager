package com.bandage.bandmanager.domain.jam.scheduler

import com.bandage.bandmanager.domain.jam.model.Jam
import com.bandage.bandmanager.domain.jam.repository.JamRepository
import com.bandage.bandmanager.domain.notify.repository.NotificationRepository
import com.bandage.bandmanager.global.async.event.CommonEvent
import com.bandage.bandmanager.global.async.publisher.EventPublisher
import com.bandage.bandmanager.global.common.domain.TrackInfo
import com.bandage.bandmanager.global.notify.annotation.NotifyCategory
import com.bandage.bandmanager.global.notify.event.NotificationCreateEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.UUID

class JamReminderSchedulerTest {
    private val jamRepository = mock(JamRepository::class.java)
    private val notificationRepository = mock(NotificationRepository::class.java)

    // EventPublisher 는 fake 로 두어 발행 이벤트를 직접 캡처한다(Kotlin+Mockito any() NPE 회피).
    private val published = mutableListOf<CommonEvent>()
    private val eventPublisher =
        object : EventPublisher {
            override fun publish(event: CommonEvent) {
                published.add(event)
            }
        }
    private val sut = JamReminderScheduler(jamRepository, notificationRepository, eventPublisher)

    // non-null 파라미터용 매처: Mockito eq()/any() 가 null 을 반환하면 Kotlin 이 호출 지점에서
    // null 체크를 삽입해 NPE 가 나므로 elvis 로 non-null 을 보장한다(매처는 정상 등록됨).
    private fun anyLocalDateTime(): LocalDateTime = Mockito.any(LocalDateTime::class.java) ?: LocalDateTime.now()

    private fun eqCategory(value: NotifyCategory): NotifyCategory = eq(value) ?: value

    private fun jam(vararg members: Long): Jam {
        val jam =
            Jam.create(
                title = "정기합주",
                trackInfo = TrackInfo(title = "곡", artist = "아티스트"),
                startAt = LocalDateTime.now().plusHours(24),
                durationMinutes = 60,
                venue = null,
            )
        setId(jam, UUID.randomUUID())
        members.forEachIndexed { idx, member -> jam.addParticipant("session-$idx", member) }
        return jam
    }

    private fun setId(
        target: Any,
        id: UUID,
    ) {
        val field = target.javaClass.getDeclaredField("id")
        field.isAccessible = true
        field.set(target, id)
    }

    private fun publishedRecipients(): List<Long> =
        published.flatMap { (it as NotificationCreateEvent).payloads.map { p -> p.recipientId } }

    @Test
    fun `임박 합주 참여자에게 알림 페이로드를 발행한다`() {
        val jam = jam(1L, 2L)
        `when`(jamRepository.findUpcomingBetween(anyLocalDateTime(), anyLocalDateTime())).thenReturn(listOf(jam))
        `when`(
            notificationRepository.existsByRecipientIdAndCategoryAndReferenceId(
                anyLong(),
                eqCategory(NotifyCategory.JAM_UPCOMING),
                anyString(),
            ),
        ).thenReturn(false)

        sut.remindUpcomingJams()

        assertThat(publishedRecipients()).containsExactlyInAnyOrder(1L, 2L)
    }

    @Test
    fun `이미 알림이 있는 참여자는 제외된다(멱등성)`() {
        val jam = jam(1L, 2L)
        `when`(jamRepository.findUpcomingBetween(anyLocalDateTime(), anyLocalDateTime())).thenReturn(listOf(jam))
        `when`(
            notificationRepository.existsByRecipientIdAndCategoryAndReferenceId(
                eq(1L),
                eqCategory(NotifyCategory.JAM_UPCOMING),
                anyString(),
            ),
        ).thenReturn(true)
        `when`(
            notificationRepository.existsByRecipientIdAndCategoryAndReferenceId(
                eq(2L),
                eqCategory(NotifyCategory.JAM_UPCOMING),
                anyString(),
            ),
        ).thenReturn(false)

        sut.remindUpcomingJams()

        assertThat(publishedRecipients()).containsExactly(2L)
    }

    @Test
    fun `임박 합주가 없으면 발행하지 않는다`() {
        `when`(jamRepository.findUpcomingBetween(anyLocalDateTime(), anyLocalDateTime())).thenReturn(emptyList())

        sut.remindUpcomingJams()

        assertThat(published).isEmpty()
    }
}
