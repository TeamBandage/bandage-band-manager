package com.bandage.bandmanager.domain.schedule_bak.dto.res

import com.bandage.bandmanager.domain.schedule_bak.model.MemberSchedule
import java.time.LocalDate
import java.time.LocalDateTime

data class MemberScheduleResponse(
    val userId: Long,
    val availableDates: Set<LocalDate>,
    val unavailableDates: Set<LocalDate>,
    val blocks: Map<LocalDate, String>,
    val note: String?,
    val completed: Boolean,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun from(schedule: MemberSchedule): MemberScheduleResponse =
            MemberScheduleResponse(
                userId = schedule.userId,
                availableDates = schedule.availableDates,
                unavailableDates = schedule.unavailableDates,
                blocks = schedule.blocks,
                note = schedule.note,
                completed = schedule.completed,
                updatedAt = schedule.lastModifiedAt,
            )

        fun empty(userId: Long): MemberScheduleResponse =
            MemberScheduleResponse(
                userId = userId,
                availableDates = emptySet(),
                unavailableDates = emptySet(),
                blocks = emptyMap(),
                note = null,
                completed = false,
                updatedAt = null,
            )
    }
}
