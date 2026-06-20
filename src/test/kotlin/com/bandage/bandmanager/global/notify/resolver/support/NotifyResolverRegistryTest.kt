package com.bandage.bandmanager.global.notify.resolver.support

import com.bandage.bandmanager.global.notify.annotation.NotifyCategory
import com.bandage.bandmanager.global.notify.resolver.NotificationPayload
import com.bandage.bandmanager.global.notify.resolver.NotifyPayloadResolver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NotifyResolverRegistryTest {
    private fun resolverOf(
        cat: NotifyCategory,
        payloads: List<NotificationPayload>,
    ): NotifyPayloadResolver =
        object : NotifyPayloadResolver {
            override val category = cat

            override fun resolve(
                args: Array<Any?>,
                returnValue: Any?,
            ): List<NotificationPayload> = payloads
        }

    @Test
    fun `카테고리에 맞는 resolver 로 디스패치한다`() {
        val payload = NotificationPayload(1L, NotifyCategory.BAND_APPLICATION, "제목", "메시지")
        val registry = NotifyResolverRegistry(listOf(resolverOf(NotifyCategory.BAND_APPLICATION, listOf(payload))))

        val result = registry.resolve(NotifyCategory.BAND_APPLICATION, arrayOf<Any?>(), null)

        assertThat(result).containsExactly(payload)
    }

    @Test
    fun `등록되지 않은 카테고리는 빈 리스트를 반환한다`() {
        val registry = NotifyResolverRegistry(emptyList())

        val result = registry.resolve(NotifyCategory.JAM_UPCOMING, arrayOf<Any?>(), null)

        assertThat(result).isEmpty()
    }
}
