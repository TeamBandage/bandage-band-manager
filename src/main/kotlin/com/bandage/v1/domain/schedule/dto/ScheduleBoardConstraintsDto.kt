package com.bandage.v1.domain.schedule.dto

import com.bandage.v1.domain.schedule.model.ScheduleBoardConstraints

data class ScheduleBoardConstraintsDto(
    val workingHoursStart: Int = ScheduleBoardConstraints.DEFAULT_WORKING_HOURS_START,
    val workingHoursEnd: Int = ScheduleBoardConstraints.DEFAULT_WORKING_HOURS_END,
    val excludeLateNight: Boolean = true,
    val maxConsecutiveMinutes: Int = ScheduleBoardConstraints.DEFAULT_MAX_CONSECUTIVE_MINUTES,
) {
    fun toEntity(): ScheduleBoardConstraints =
        ScheduleBoardConstraints(
            workingHoursStart = workingHoursStart,
            workingHoursEnd = workingHoursEnd,
            excludeLateNight = excludeLateNight,
            maxConsecutiveMinutes = maxConsecutiveMinutes,
        )

    companion object {
        fun from(constraints: ScheduleBoardConstraints): ScheduleBoardConstraintsDto =
            ScheduleBoardConstraintsDto(
                workingHoursStart = constraints.workingHoursStart,
                workingHoursEnd = constraints.workingHoursEnd,
                excludeLateNight = constraints.excludeLateNight,
                maxConsecutiveMinutes = constraints.maxConsecutiveMinutes,
            )
    }
}
