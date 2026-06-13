package com.bandage.bandmanager.global.common.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.time.LocalDateTime

@Embeddable
class TimeInfoUnit(
    startAt: LocalDateTime,
    durationMinutes: Int = 60,
    venue: String? = null,
) {
    @Column(name = "start_at", nullable = false)
    var startAt: LocalDateTime = startAt
        protected set

    @Column(name = "duration_minutes", nullable = false)
    var durationMinutes: Int = durationMinutes
        protected set

    @Column(name = "venue", nullable = true)
    var venue: String? = venue
        protected set

    fun updateTimeInfo(
        startAt: LocalDateTime,
        durationMinutes: Int,
    ) {
        this.startAt = startAt
        this.durationMinutes = durationMinutes
    }

    fun updateVenue(venue: String) {
        this.venue = venue
    }
}
