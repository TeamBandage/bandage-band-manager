package com.bandage.bandmanager.domain.schedule.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.time.LocalDate

@Embeddable
data class ScheduleWindow(
    @Column(name = "scheudle_window_from", nullable = false)
    val from: LocalDate,
    @Column(name = "schedule_window_to", nullable = false)
    val to: LocalDate,
) {
    init {
        require(!from.isAfter(to)) { "scheduleWindow.from must be on or before scheduleWindow.to" }
    }
}
