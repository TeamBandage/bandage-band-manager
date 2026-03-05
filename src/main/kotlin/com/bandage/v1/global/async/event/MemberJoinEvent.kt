package com.bandage.v1.global.async.event

import com.bandage.v1.domain.member.model.Member
import com.bandage.v1.global.common.domain.enums.MemberRole

data class MemberJoinEvent(
    val memberId: Long,
    val memberEmail: String,
    val memberPassword: String,
    val memberRole: MemberRole,
) : CommonEvent(
        eventType = EventType.MEMBER_JOIN,
        aggregateId = memberId.toString(),
    ) {
    companion object {
        fun of(member: Member): MemberJoinEvent =
            MemberJoinEvent(
                memberId = member.id!!,
                memberEmail = member.email,
                memberPassword = member.password,
                memberRole = member.role,
            )
    }
}
