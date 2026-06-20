package com.bandage.bandmanager.global.notify.annotation

/**
 * 알림 카테고리. 프론트 필터링 키이자 @Notify 트리거의 분류 기준.
 *
 * 추가/삭제가 빈번하므로 DB 에는 @Enumerated(EnumType.STRING) 으로 저장한다
 * (ORDINAL 은 순서 변경 시 기존 데이터 의미가 깨진다).
 */
enum class NotifyCategory {
    BAND_APPLICATION, // 가입 신청 발생 → 밴드 리더(들)에게
    BAND_APPLICATION_RESULT, // 신청 승인/거절 결과 → 신청자에게
    AUTHORITY_PROMOTION, // 권한 승격(리더 승격 등) → 승격된 멤버에게
    JAM_UPCOMING, // 임박한 합주 → 참여자에게
}
