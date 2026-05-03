package com.bandage.v1.domain.schedule.model

import java.io.Serializable
import java.util.UUID

data class MemberScheduleId(
    val meetingId: UUID = UUID(0, 0),
    val userId: Long = 0,
) : Serializable
