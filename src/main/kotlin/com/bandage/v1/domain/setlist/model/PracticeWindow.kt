package com.bandage.v1.domain.setlist.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.time.LocalDate

@Embeddable
data class PracticeWindow(
    @Column(name = "practice_window_from", nullable = false)
    val from: LocalDate,
    @Column(name = "practice_window_to", nullable = false)
    val to: LocalDate,
) {
    init {
        require(!from.isAfter(to)) { "practiceWindow.from must be on or before practiceWindow.to" }
    }
}
