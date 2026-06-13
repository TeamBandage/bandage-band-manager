package com.bandage.bandmanager.domain.schedule.dto.res

import java.time.LocalDate

data class MemberScheduleAggregateResponse(
    val dateAvailability: Map<LocalDate, AvailabilityCount>,
    val totalParticipants: Int,
    val completedCount: Int,
) {
    data class AvailabilityCount(
        val available: Int,
        val unavailable: Int,
        val pending: Int,
    )
}
