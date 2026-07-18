package com.bandage.bandmanager.domain.performance.notify

import com.bandage.bandmanager.domain.performance.repository.PerformanceRepository
import com.bandage.bandmanager.global.notify.annotation.NotifyCategory
import com.bandage.bandmanager.global.notify.resolver.NotificationPayload
import com.bandage.bandmanager.global.notify.resolver.NotifyPayloadResolver
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * PERFORMANCE_OWNER_PROMOTED: 공연 소유권을 넘겨받은 새 소유자에게 알림.
 * 트리거: PerformanceService.delegateOwnership(performanceId: UUID, targetMemberId: Long, ownerId: Long)
 */
@Component
class PerformanceOwnerPromotedResolver(
    private val performanceRepository: PerformanceRepository,
) : NotifyPayloadResolver {
    override val category = NotifyCategory.PERFORMANCE_OWNER_PROMOTED

    override fun resolve(
        args: Array<Any?>,
        returnValue: Any?,
    ): List<NotificationPayload> {
        val performanceId = args[0] as UUID
        val targetMemberId = args[1] as Long
        val performance = performanceRepository.findByIdOrNull(performanceId) ?: return emptyList()

        return listOf(
            NotificationPayload(
                recipientId = targetMemberId,
                category = category,
                title = "공연 소유자 승격",
                message = "'${performance.title}' 공연의 새로운 소유자로 승격되었습니다.",
                referenceId = performanceId.toString(),
            ),
        )
    }
}
