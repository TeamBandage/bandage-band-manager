package com.bandage.bandmanager.domain.band.notify

import com.bandage.bandmanager.domain.band.model.enums.BandRole
import com.bandage.bandmanager.domain.band.repository.BandMemberRepository
import com.bandage.bandmanager.global.notify.annotation.NotifyCategory
import com.bandage.bandmanager.global.notify.resolver.NotificationPayload
import com.bandage.bandmanager.global.notify.resolver.NotifyPayloadResolver
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * BAND_APPLICATION: 가입 신청 발생 시 밴드 리더(들)에게 알림.
 * 트리거: BandService.createBandApplication(bandId: UUID, memberId: Long)
 */
@Component
class BandApplicationResolver(
    private val bandMemberRepository: BandMemberRepository,
) : NotifyPayloadResolver {
    override val category = NotifyCategory.BAND_APPLICATION

    override fun resolve(
        args: Array<Any?>,
        returnValue: Any?,
    ): List<NotificationPayload> {
        val bandId = args[0] as UUID
        return bandMemberRepository
            .findAllByBandIdAndRole(bandId, BandRole.LEADER)
            .map { leader ->
                NotificationPayload(
                    recipientId = leader.member,
                    category = category,
                    title = "새로운 가입 신청",
                    message = "${leader.band.name} 밴드에 새로운 가입 신청이 도착했습니다.",
                    referenceId = bandId.toString(),
                )
            }
    }
}
