package com.bandage.bandmanager.global.notify.resolver

import com.bandage.bandmanager.global.notify.annotation.NotifyCategory

/**
 * 카테고리별 수신자/내용 추출 전략.
 *
 * 구현체는 해당 도메인 엔티티를 직접 다루므로 Modulith 경계를 위해 각 도메인 패키지
 * (예: domain/band/notify/) 에 둔다. 인터페이스만 global 에 둔다(의존 방향 domain → global).
 */
interface NotifyPayloadResolver {
    val category: NotifyCategory

    /**
     * @param args 대상 메서드 인자 배열(JoinPoint.args)
     * @param returnValue 대상 메서드 반환값(void 이면 null)
     * @return 생성할 알림 목록(수신자별 1건). 비어 있으면 알림 미생성.
     */
    fun resolve(
        args: Array<Any?>,
        returnValue: Any?,
    ): List<NotificationPayload>
}
