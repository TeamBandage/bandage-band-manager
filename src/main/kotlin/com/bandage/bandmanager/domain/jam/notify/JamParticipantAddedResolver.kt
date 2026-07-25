package com.bandage.bandmanager.domain.jam.notify

import com.bandage.bandmanager.domain.jam.dto.req.JamMemberAddRequest
import com.bandage.bandmanager.domain.jam.repository.JamRepository
import com.bandage.bandmanager.domain.notify.repository.NotificationRepository
import com.bandage.bandmanager.global.notify.annotation.NotifyCategory
import com.bandage.bandmanager.global.notify.resolver.NotificationPayload
import com.bandage.bandmanager.global.notify.resolver.NotifyPayloadResolver
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * JAM_PARTICIPANT_ADDED: 합주 참여자로 추가된 멤버에게 알림.
 * 트리거: JamService.addParticipant(jamId: UUID, request: JamMemberAddRequest, memberId: Long)
 *
 * addParticipant 는 세션 배정마다 호출되어 동일 멤버가 여러 번 지나므로,
 * (recipient, JAM_PARTICIPANT_ADDED, jamId) 멱등성으로 합주당 1회만 생성한다.
 */
@Component
class JamParticipantAddedResolver(
    private val jamRepository: JamRepository,
    private val notificationRepository: NotificationRepository,
) : NotifyPayloadResolver {
    override val category = NotifyCategory.JAM_PARTICIPANT_ADDED

    override fun resolve(
        args: Array<Any?>,
        returnValue: Any?,
    ): List<NotificationPayload> {
        val jamId = args[0] as UUID
        val request = args[1] as JamMemberAddRequest
        val jam = jamRepository.findByIdOrNull(jamId) ?: return emptyList()

        if (notificationRepository.existsByRecipientIdAndCategoryAndReferenceId(
                request.memberId,
                category,
                jamId.toString(),
            )
        ) {
            return emptyList()
        }

        return listOf(
            NotificationPayload(
                recipientId = request.memberId,
                category = category,
                title = "합주 참여자 추가",
                message = "${jam.title} 합주에 참여자로 추가되었습니다.",
                referenceId = jamId.toString(),
            ),
        )
    }
}
