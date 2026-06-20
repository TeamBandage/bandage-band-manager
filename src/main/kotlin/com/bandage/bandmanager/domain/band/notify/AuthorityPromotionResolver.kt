package com.bandage.bandmanager.domain.band.notify

import com.bandage.bandmanager.domain.band.repository.BandMemberRepository
import com.bandage.bandmanager.global.notify.annotation.NotifyCategory
import com.bandage.bandmanager.global.notify.resolver.NotificationPayload
import com.bandage.bandmanager.global.notify.resolver.NotifyPayloadResolver
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * AUTHORITY_PROMOTION: 리더 승격 시 새 리더에게 알림.
 * 트리거: BandService.changeLeader(bandId, bandMemberId, memberId) — bandMemberId 가 새 리더.
 */
@Component
class AuthorityPromotionResolver(
    private val bandMemberRepository: BandMemberRepository,
) : NotifyPayloadResolver {
    override val category = NotifyCategory.AUTHORITY_PROMOTION

    override fun resolve(
        args: Array<Any?>,
        returnValue: Any?,
    ): List<NotificationPayload> {
        val bandId = args[0] as UUID
        val bandMemberId = args[1] as UUID
        val newLeader = bandMemberRepository.findByIdOrNull(bandMemberId) ?: return emptyList()

        return listOf(
            NotificationPayload(
                recipientId = newLeader.member,
                category = category,
                title = "밴드 리더 승격",
                message = "밴드의 새로운 리더로 승격되었습니다.",
                referenceId = bandId.toString(),
            ),
        )
    }
}
