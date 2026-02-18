package com.bandage.v1.domain.performance.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Entity
@Table(name = "p_performance")
class Performance(
    @Column(name = "title", nullable = false)
    var title: String,
    @Column(name = "start_at", nullable = false)
    var startAt: LocalDateTime = defaultStartTime(),
    @Column(name = "duration_minutes", nullable = false)
    var durationMinutes: Int = 60,
    @Column(name = "venue", nullable = true)
    var venue: String? = null,
    @OneToMany(mappedBy = "performance", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    var setlist: MutableList<SetlistItem> = mutableListOf(),
) {
    @Id
    @Column(name = "performance_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    var id: UUID? = null

    companion object {
        private fun defaultStartTime(): LocalDateTime {
            return LocalDateTime.now()
                .plusMonths(1)
                .truncatedTo(ChronoUnit.HOURS)
        }
    }
}