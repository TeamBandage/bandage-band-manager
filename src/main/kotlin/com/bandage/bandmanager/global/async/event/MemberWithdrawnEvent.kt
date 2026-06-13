package com.bandage.bandmanager.global.async.event

@Deprecated(message = "회원 인증 로직: 이벤트 기반 아키텍처 적용 X")
data class MemberWithdrawnEvent(
    val memberId: Long,
) : CommonEvent(
        eventType = EventType.MEMBER_WITHDRAW,
        aggregateId = memberId.toString(),
    ) {
//    companion object {
//        fun of(memberId: Long): MemberWithdrawnEvent =
//            MemberWithdrawnEvent(
//                memberId = memberId,
//            )
//    }
}
