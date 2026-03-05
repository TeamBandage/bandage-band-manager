package com.bandage.v1.global.async.event

data class MemberLoginEvent(
    val memberId: Long,
    val refreshToken: String,
) : CommonEvent(
        eventType = EventType.MEMBER_LOGIN,
        aggregateId = memberId.toString(),
    ) {
    companion object {
        fun of(
            memberId: Long,
            refreshToken: String,
        ): MemberLoginEvent =
            MemberLoginEvent(
                memberId = memberId,
                refreshToken = refreshToken,
            )
    }
}
