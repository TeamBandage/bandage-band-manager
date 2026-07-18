package com.bandage.bandmanager.domain.jam.notify

import com.bandage.bandmanager.domain.jam.dto.res.JamResponse
import com.bandage.bandmanager.domain.jam.repository.JamParticipantRepository
import com.bandage.bandmanager.global.notify.annotation.NotifyCategory
import com.bandage.bandmanager.global.notify.resolver.NotificationPayload
import com.bandage.bandmanager.global.notify.resolver.NotifyPayloadResolver
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * JAM_CREATED: 셋리스트 기반으로 합주가 일괄 생성되면, 생성된 합주들의 참여자 전원에게
 * "N 건의 합주가 생성되었습니다" 요약 알림을 참여자당 1건씩 보낸다(N = 생성된 합주 개수).
 * 트리거: JamCreateFromSetlistFacade.createJamsFromSetlist(memberId: Long, setlistId: UUID, request): List<JamResponse>
 */
@Component
class JamCreatedFromSetlistResolver(
    private val jamParticipantRepository: JamParticipantRepository,
) : NotifyPayloadResolver {
    override val category = NotifyCategory.JAM_CREATED

    override fun resolve(
        args: Array<Any?>,
        returnValue: Any?,
    ): List<NotificationPayload> {
        val setlistId = args[1] as UUID
        val jamIds = (returnValue as? List<*>).orEmpty().filterIsInstance<JamResponse>().map { it.jamId }
        if (jamIds.isEmpty()) return emptyList()

        val count = jamIds.size
        return jamParticipantRepository
            .findAllByJamIdIn(jamIds)
            .map { it.member }
            .distinct()
            .map { memberId ->
                NotificationPayload(
                    recipientId = memberId,
                    category = category,
                    title = "합주 생성",
                    message = "${count}건의 합주가 생성되었습니다.",
                    referenceId = setlistId.toString(),
                )
            }
    }
}
