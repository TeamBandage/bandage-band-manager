package com.bandage.v1.global.async.event

import com.bandage.v1.domain.auth.model.enums.MemberRole

@Deprecated(message = "회원 인증 로직: 이벤트 기반 아키텍처 적용 X")
data class MemberJoinEvent(
    val memberId: Long,
    val memberEmail: String,
    val memberPassword: String,
    val memberRole: MemberRole,
) : CommonEvent(
        eventType = EventType.MEMBER_JOIN,
        aggregateId = memberId.toString(),
    ) {
//    companion object {
//        fun of(member: Member): MemberJoinEvent =
//            MemberJoinEvent(
//                memberId = member.id!!,
//                memberEmail = member.email,
//                memberPassword = member.password,
//                memberRole = member.role,
//            )
//    }
}
