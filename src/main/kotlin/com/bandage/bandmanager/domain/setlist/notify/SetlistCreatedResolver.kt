package com.bandage.bandmanager.domain.setlist.notify

import com.bandage.bandmanager.domain.setlist.dto.res.SetlistResponse
import com.bandage.bandmanager.domain.setlist.repository.SetlistTrackParticipantRepository
import com.bandage.bandmanager.domain.setlist.repository.SetlistTrackRepository
import com.bandage.bandmanager.global.notify.annotation.NotifyCategory
import com.bandage.bandmanager.global.notify.resolver.NotificationPayload
import com.bandage.bandmanager.global.notify.resolver.NotifyPayloadResolver
import org.springframework.stereotype.Component

/**
 * SETLIST_CREATED: 셋리스트가 생성되면 트랙 참여자(세션 확정자) 전원에게 알림.
 * 트리거: SetlistCreateFacade.createSetlist(memberId: Long, request: SetlistCreateRequest): SetlistResponse
 *
 * 생성 결과 setlistId/이름은 반환값(SetlistResponse)에서 얻고, 수신자는 저장된 트랙 참여자로부터 조회한다.
 */
@Component
class SetlistCreatedResolver(
    private val setlistTrackRepository: SetlistTrackRepository,
    private val setlistTrackParticipantRepository: SetlistTrackParticipantRepository,
) : NotifyPayloadResolver {
    override val category = NotifyCategory.SETLIST_CREATED

    override fun resolve(
        args: Array<Any?>,
        returnValue: Any?,
    ): List<NotificationPayload> {
        val response = returnValue as? SetlistResponse ?: return emptyList()

        val tracks = setlistTrackRepository.findAllBySetlistIdIn(listOf(response.setlistId))
        if (tracks.isEmpty()) return emptyList()

        return setlistTrackParticipantRepository
            .findAllByTrackIn(tracks)
            .map { it.memberId }
            .distinct()
            .map { memberId ->
                NotificationPayload(
                    recipientId = memberId,
                    category = category,
                    title = "셋리스트 생성",
                    message = "'${response.title}' 셋리스트가 생성되었습니다.",
                    referenceId = response.setlistId.toString(),
                )
            }
    }
}
