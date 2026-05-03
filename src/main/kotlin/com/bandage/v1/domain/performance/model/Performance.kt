package com.bandage.v1.domain.performance.model

import com.bandage.v1.global.common.domain.BaseEntity
import com.bandage.v1.global.common.domain.TimeInfoUnit
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.UuidGenerator
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "p_performance")
@SQLRestriction("deleted_at IS NULL")
open class Performance(
    title: String,
    timeInfo: TimeInfoUnit,
) : BaseEntity() {
    @Id
    @Column(name = "performance_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @Column(name = "title", nullable = false)
    var title: String = title
        protected set

    @Embedded
    var timeInfo: TimeInfoUnit = timeInfo
        protected set

    @OneToMany(mappedBy = "performance", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    private var _bands: MutableList<PerformanceBand> = mutableListOf()

    @OneToMany(mappedBy = "performance", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    private var _managers: MutableList<PerformanceManager> = mutableListOf()

    @OneToMany(mappedBy = "performance", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    private var _practices: MutableList<PerformancePractice> = mutableListOf()

    val bands: List<PerformanceBand> get() = _bands.toList()
    val managers: List<PerformanceManager> get() = _managers.toList()
    val practices: List<PerformancePractice> get() = _practices.toList()

    companion object {
        fun create(
            title: String,
            startAt: LocalDateTime,
            durationMinutes: Int,
            venue: String?,
        ): Performance =
            Performance(
                title = title,
                timeInfo = TimeInfoUnit(startAt = startAt, durationMinutes = durationMinutes, venue = venue),
            )
    }

    fun updateTitle(newTitle: String) {
        this.title = newTitle
    }

    fun updateTimeInfo(
        startAt: LocalDateTime,
        durationMinutes: Int,
    ) {
        timeInfo.updateTimeInfo(startAt, durationMinutes)
    }

    fun updateVenue(newVenue: String) {
        timeInfo.updateVenue(newVenue)
    }

    fun addBand(band: PerformanceBand) {
        _bands.add(band)
    }

    fun removeBand(band: PerformanceBand) {
        _bands.remove(band)
    }

    fun addManager(manager: PerformanceManager) {
        _managers.add(manager)
    }

    fun removeManager(manager: PerformanceManager) {
        _managers.remove(manager)
    }

    fun addPractice(performancePractice: PerformancePractice) {
        _practices.add(performancePractice)
    }

    fun removePractice(performancePractice: PerformancePractice) {
        _practices.remove(performancePractice)
    }
}
