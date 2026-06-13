package com.bandage.bandmanager.global.async.event

@Deprecated(message = "회원 인증 로직: 이벤트 기반 아키텍처 적용 X")
data class MemberLoginEvent(
    val memberId: Long,
    val refreshToken: String,
) : CommonEvent(
        eventType = EventType.MEMBER_LOGIN,
        aggregateId = memberId.toString(),
    ) {
//    companion object {
//        fun of(
//            memberId: Long,
//            refreshToken: String,
//        ): MemberLoginEvent =
//            MemberLoginEvent(
//                memberId = memberId,
//                refreshToken = refreshToken,
//            )
//    }
}
