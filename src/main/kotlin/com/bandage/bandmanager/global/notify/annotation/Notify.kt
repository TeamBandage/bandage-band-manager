package com.bandage.bandmanager.global.notify.annotation

/**
 * 서비스 메서드에 부착하면 정상 반환 시 해당 [category] 의 알림이 생성된다.
 *
 * 수신자/내용은 카테고리별 NotifyPayloadResolver 가 메서드 인자/반환값에서 추출한다.
 * 부착 메서드는 반드시 @Transactional 컨텍스트여야 한다(AFTER_COMMIT 리스너 동작 보장).
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Notify(
    val category: NotifyCategory,
)
