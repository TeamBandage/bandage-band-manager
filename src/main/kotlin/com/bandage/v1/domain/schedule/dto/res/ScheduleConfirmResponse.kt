package com.bandage.v1.domain.schedule.dto.res

import java.time.LocalDateTime
import java.util.UUID

data class ScheduleConfirmResponse(
    val confirmedAt: LocalDateTime,
    val practicesCreated: List<PracticeCreatedSummary>,
    val performancePracticesLinked: List<PerformancePracticeSummary>,
) {
    data class PracticeCreatedSummary(
        val practiceId: UUID,
        val title: String,
        val startAt: LocalDateTime,
        val durationMinutes: Int,
    )

    data class PerformancePracticeSummary(
        val performancePracticeId: UUID,
        val performanceId: UUID,
        val practiceId: UUID,
    )
}
