package com.bandage.bandmanager.domain.schedule.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
open class ScheduleBoardConstraints(
    workingHoursStart: Int = DEFAULT_WORKING_HOURS_START,
    workingHoursEnd: Int = DEFAULT_WORKING_HOURS_END,
    excludeLateNight: Boolean = true,
    maxConsecutiveMinutes: Int = DEFAULT_MAX_CONSECUTIVE_MINUTES,
) {
    @Column(name = "working_hours_start", nullable = false)
    var workingHoursStart: Int = workingHoursStart
        protected set

    @Column(name = "working_hours_end", nullable = false)
    var workingHoursEnd: Int = workingHoursEnd
        protected set

    @Column(name = "exclude_late_night", nullable = false)
    var excludeLateNight: Boolean = excludeLateNight
        protected set

    @Column(name = "max_consecutive_minutes", nullable = false)
    var maxConsecutiveMinutes: Int = maxConsecutiveMinutes
        protected set

    fun update(
        workingHoursStart: Int?,
        workingHoursEnd: Int?,
        excludeLateNight: Boolean?,
        maxConsecutiveMinutes: Int?,
    ) {
        workingHoursStart?.let { this.workingHoursStart = it }
        workingHoursEnd?.let { this.workingHoursEnd = it }
        excludeLateNight?.let { this.excludeLateNight = it }
        maxConsecutiveMinutes?.let { this.maxConsecutiveMinutes = it }
    }

    companion object {
        const val DEFAULT_WORKING_HOURS_START = 18
        const val DEFAULT_WORKING_HOURS_END = 44
        const val DEFAULT_MAX_CONSECUTIVE_MINUTES = 240
    }
}
