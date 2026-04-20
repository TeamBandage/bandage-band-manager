package com.bandage.v1.domain.performance.model

import com.bandage.v1.global.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(name = "p_performance_manager")
open class PerformanceManager(
    performance: Performance,
    member: Long,
) : BaseEntity() {
    @Id
    @Column(name = "performance_manager_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id", nullable = false)
    val performance: Performance = performance

    @Column(name = "member_id", nullable = false)
    val member: Long = member

    companion object {
        fun create(
            performance: Performance,
            member: Long,
        ): PerformanceManager =
            PerformanceManager(
                performance = performance,
                member = member,
            )
    }
}
