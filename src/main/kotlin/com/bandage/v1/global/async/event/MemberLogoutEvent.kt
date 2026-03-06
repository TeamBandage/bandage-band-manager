package com.bandage.v1.global.async.event

@Deprecated(message = "회원 인증 로직: 이벤트 기반 아키텍처 적용 X")
data class MemberLogoutEvent(
    val memberId: Long,
) : CommonEvent(
        eventType = EventType.MEMBER_LOGOUT,
        aggregateId = memberId.toString(),
    ) {
//    companion object {
//        fun of(memberId: Long): MemberLogoutEvent =
//            MemberLogoutEvent(
//                memberId = memberId,
//            )
//    }
}
