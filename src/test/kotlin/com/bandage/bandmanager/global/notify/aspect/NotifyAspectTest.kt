package com.bandage.bandmanager.global.notify.aspect

import com.bandage.bandmanager.global.async.event.CommonEvent
import com.bandage.bandmanager.global.async.publisher.EventPublisher
import com.bandage.bandmanager.global.notify.annotation.Notify
import com.bandage.bandmanager.global.notify.annotation.NotifyCategory
import com.bandage.bandmanager.global.notify.event.NotificationCreateEvent
import com.bandage.bandmanager.global.notify.resolver.NotificationPayload
import com.bandage.bandmanager.global.notify.resolver.NotifyPayloadResolver
import com.bandage.bandmanager.global.notify.resolver.support.NotifyResolverRegistry
import org.aspectj.lang.JoinPoint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class NotifyAspectTest {
    // EventPublisher 는 mock 대신 fake 로 두어 발행 이벤트를 직접 캡처한다(Kotlin+Mockito any() NPE 회피).
    private val published = mutableListOf<CommonEvent>()
    private val eventPublisher =
        object : EventPublisher {
            override fun publish(event: CommonEvent) {
                published.add(event)
            }
        }

    private val joinPoint = mock(JoinPoint::class.java)
    private val notify = mock(Notify::class.java)

    private fun aspectReturning(payloads: List<NotificationPayload>): NotifyAspect {
        val registry =
            NotifyResolverRegistry(
                listOf(
                    object : NotifyPayloadResolver {
                        override val category = NotifyCategory.BAND_APPLICATION

                        override fun resolve(
                            args: Array<Any?>,
                            returnValue: Any?,
                        ): List<NotificationPayload> = payloads
                    },
                ),
            )
        return NotifyAspect(registry, eventPublisher)
    }

    @Test
    fun `payload 가 있으면 이벤트를 발행한다`() {
        `when`(notify.category).thenReturn(NotifyCategory.BAND_APPLICATION)
        `when`(joinPoint.args).thenReturn(arrayOf<Any?>())
        val sut = aspectReturning(listOf(NotificationPayload(1L, NotifyCategory.BAND_APPLICATION, "제목", "메시지")))

        sut.afterReturning(joinPoint, notify, null)

        assertThat(published).hasSize(1)
        assertThat(published[0]).isInstanceOf(NotificationCreateEvent::class.java)
    }

    @Test
    fun `payload 가 비어있으면 발행하지 않는다`() {
        `when`(notify.category).thenReturn(NotifyCategory.BAND_APPLICATION)
        `when`(joinPoint.args).thenReturn(arrayOf<Any?>())
        val sut = aspectReturning(emptyList())

        sut.afterReturning(joinPoint, notify, null)

        assertThat(published).isEmpty()
    }
}
