package com.bandage.bandmanager.domain.selection.notify

import com.bandage.bandmanager.domain.selection.dto.req.SetlistParticipantsUpdateRequest
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionRepository
import com.bandage.bandmanager.global.notify.annotation.NotifyCategory
import com.bandage.bandmanager.global.notify.resolver.NotificationPayload
import com.bandage.bandmanager.global.notify.resolver.NotifyPayloadResolver
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * SELECTION_PARTICIPANT_ADDED: 선곡회의 참여자로 추가된 멤버(들)에게 알림.
 * 트리거: TrackSelectionService.updateParticipants(selectionId: UUID, memberId: Long, request: SetlistParticipantsUpdateRequest)
 *
 * request.add 의 각 memberId 가 신규 추가 대상(수신자)이다. remove 만 있으면 알림 없음.
 */
@Component
class SelectionParticipantAddedResolver(
    private val trackSelectionRepository: TrackSelectionRepository,
) : NotifyPayloadResolver {
    override val category = NotifyCategory.SELECTION_PARTICIPANT_ADDED

    override fun resolve(
        args: Array<Any?>,
        returnValue: Any?,
    ): List<NotificationPayload> {
        val selectionId = args[0] as UUID
        val request = args[2] as SetlistParticipantsUpdateRequest
        if (request.add.isEmpty()) return emptyList()
        val selection = trackSelectionRepository.findByIdOrNull(selectionId) ?: return emptyList()

        return request.add
            .map { it.memberId }
            .distinct()
            .map { memberId ->
                NotificationPayload(
                    recipientId = memberId,
                    category = category,
                    title = "선곡 회의 참여자 추가",
                    message = "'${selection.title}' 선곡 회의에 참여자로 추가되었습니다.",
                    referenceId = selectionId.toString(),
                )
            }
    }
}
