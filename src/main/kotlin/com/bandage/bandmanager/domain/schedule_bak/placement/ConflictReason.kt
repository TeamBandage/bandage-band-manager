package com.bandage.bandmanager.domain.schedule_bak.placement

/**
 * 특정 멤버가 후보 슬롯에 배치될 수 없는 사유.
 */
enum class ConflictReason {
    /** 멤버의 글로벌 가용성(MemberAvailability) 상 가용 시간이 아님 */
    UNAVAILABLE,

    /** 이미 확정된 다른 Jam(JamReservation)과 시간이 겹침 */
    JAM_RESERVATION,

    /** 같은 보드에서 먼저 배치된(pending) 블록과 시간이 겹침 */
    PENDING_BLOCK,
}
