package com.bandage.v1.domain.schedule.dto.res

import java.time.LocalDateTime
import java.util.UUID

data class ScheduleConfirmResponse(
    val confirmedAt: LocalDateTime,
    val jamsCreated: List<JamCreatedSummary>,
) {
    data class JamCreatedSummary(
        val jamId: UUID,
        val title: String,
        val startAt: LocalDateTime,
        val durationMinutes: Int,
    )
}
