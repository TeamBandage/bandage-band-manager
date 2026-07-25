package com.bandage.bandmanager.domain.band.notify

import com.bandage.bandmanager.domain.band.model.enums.ApplicationStatus
import com.bandage.bandmanager.domain.band.repository.BandApplicationRepository
import com.bandage.bandmanager.global.notify.annotation.NotifyCategory
import com.bandage.bandmanager.global.notify.resolver.NotificationPayload
import com.bandage.bandmanager.global.notify.resolver.NotifyPayloadResolver
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * BAND_APPLICATION_RESULT: 가입 신청 승인/거절 결과를 신청자에게 알림.
 * 트리거: BandService.processBandApplication(bandId, bandApplicationId, memberId, status)
 *
 * @AfterReturning 시점엔 신청 상태가 이미 변경되어 있으나 신청자(member)는 불변이므로 재조회로 추출한다.
 */
@Component
class BandApplicationResultResolver(
    private val applicationRepository: BandApplicationRepository,
) : NotifyPayloadResolver {
    override val category = NotifyCategory.BAND_APPLICATION_RESULT

    override fun resolve(
        args: Array<Any?>,
        returnValue: Any?,
    ): List<NotificationPayload> {
        val bandApplicationId = args[1] as UUID
        val status = args[3] as ApplicationStatus
        val application = applicationRepository.findByIdOrNull(bandApplicationId) ?: return emptyList()

        val (title, message) =
            when (status) {
                ApplicationStatus.APPROVED -> "가입 신청 승인" to "${application.band.name} 가입 신청이 승인되었습니다."
                ApplicationStatus.REJECTED -> "가입 신청 거절" to "${application.band.name} 가입 신청이 거절되었습니다."
                else -> return emptyList()
            }

        return listOf(
            NotificationPayload(
                recipientId = application.member,
                category = category,
                title = title,
                message = message,
                referenceId = application.band.id.toString(),
            ),
        )
    }
}
