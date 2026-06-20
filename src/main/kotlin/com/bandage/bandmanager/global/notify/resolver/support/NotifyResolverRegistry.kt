package com.bandage.bandmanager.global.notify.resolver.support

import com.bandage.bandmanager.global.notify.annotation.NotifyCategory
import com.bandage.bandmanager.global.notify.resolver.NotificationPayload
import com.bandage.bandmanager.global.notify.resolver.NotifyPayloadResolver
import org.springframework.stereotype.Component

/**
 * 모든 NotifyPayloadResolver 빈을 category 로 매핑해 디스패치한다.
 * 등록되지 않은 카테고리는 빈 리스트(알림 미생성)를 반환한다.
 */
@Component
class NotifyResolverRegistry(
    resolvers: List<NotifyPayloadResolver>,
) {
    private val byCategory: Map<NotifyCategory, NotifyPayloadResolver> = resolvers.associateBy { it.category }

    fun resolve(
        category: NotifyCategory,
        args: Array<Any?>,
        returnValue: Any?,
    ): List<NotificationPayload> = byCategory[category]?.resolve(args, returnValue) ?: emptyList()
}
