package com.bandage.bandmanager.domain.performance.notify

import com.bandage.bandmanager.domain.performance.dto.req.PerformanceInvitationCreateRequest
import com.bandage.bandmanager.domain.performance.repository.PerformanceRepository
import com.bandage.bandmanager.global.notify.annotation.NotifyCategory
import com.bandage.bandmanager.global.notify.resolver.NotificationPayload
import com.bandage.bandmanager.global.notify.resolver.NotifyPayloadResolver
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * PERFORMANCE_MANAGER_INVITED: 공연 매니저로 초대받은 멤버에게 알림.
 * 트리거: PerformanceService.sendInvitation(performanceId: UUID, request: PerformanceInvitationCreateRequest, ownerId: Long)
 */
@Component
class PerformanceManagerInvitedResolver(
    private val performanceRepository: PerformanceRepository,
) : NotifyPayloadResolver {
    override val category = NotifyCategory.PERFORMANCE_MANAGER_INVITED

    override fun resolve(
        args: Array<Any?>,
        returnValue: Any?,
    ): List<NotificationPayload> {
        val performanceId = args[0] as UUID
        val request = args[1] as PerformanceInvitationCreateRequest
        val performance = performanceRepository.findByIdOrNull(performanceId) ?: return emptyList()

        return listOf(
            NotificationPayload(
                recipientId = request.memberId,
                category = category,
                title = "공연 매니저 초대",
                message = "${performance.title} 공연의 매니저로 초대되었습니다.",
                referenceId = performanceId.toString(),
            ),
        )
    }
}
