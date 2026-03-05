package com.bandage.v1.global.async.event

data class MemberLogoutEvent(
    val memberId: Long,
) : CommonEvent(
        eventType = EventType.MEMBER_LOGOUT,
        aggregateId = memberId.toString(),
    ) {
    companion object {
        fun of(memberId: Long): MemberLogoutEvent =
            MemberLogoutEvent(
                memberId = memberId,
            )
    }
}
