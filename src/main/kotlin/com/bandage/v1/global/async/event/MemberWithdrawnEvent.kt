package com.bandage.v1.global.async.event

data class MemberWithdrawnEvent(
    val memberId: Long,
) : CommonEvent(
        eventType = EventType.MEMBER_WITHDRAW,
        aggregateId = memberId.toString(),
    ) {
    companion object {
        fun of(memberId: Long): MemberWithdrawnEvent =
            MemberWithdrawnEvent(
                memberId = memberId,
            )
    }
}
