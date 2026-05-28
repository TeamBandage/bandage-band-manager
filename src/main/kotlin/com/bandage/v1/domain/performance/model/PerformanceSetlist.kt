package com.bandage.v1.domain.performance.model

import com.bandage.v1.global.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(
    name = "p_performance_setlist",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_performance_setlist",
            columnNames = ["performance_id", "setlist_id"],
        ),
    ],
)
open class PerformanceSetlist(
    performance: Performance,
    setlistId: UUID,
) : BaseEntity() {
    @Id
    @Column(name = "performance_setlist_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id", nullable = false)
    val performance: Performance = performance

    @Column(name = "setlist_id", nullable = false)
    val setlistId: UUID = setlistId

    companion object {
        fun create(
            performance: Performance,
            setlistId: UUID,
        ): PerformanceSetlist =
            PerformanceSetlist(
                performance = performance,
                setlistId = setlistId,
            )
    }
}
