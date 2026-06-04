package com.bandage.v1.domain.performance.model

import com.bandage.v1.domain.performance.model.enums.PerformanceRole
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
    name = "p_performance_manager",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_performance_manager",
            columnNames = ["performance_id", "member_id"],
        ),
    ],
)
open class PerformanceManager(
    performance: Performance,
    member: Long,
    role: PerformanceRole,
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

    @Column(name = "role", nullable = false)
    val role: PerformanceRole = role

    fun isOwner(): Boolean = role == PerformanceRole.OWNER

    companion object {
        fun createOwner(
            performance: Performance,
            member: Long,
        ): PerformanceManager =
            PerformanceManager(
                performance = performance,
                member = member,
                role = PerformanceRole.OWNER,
            )

        fun createManager(
            performance: Performance,
            member: Long,
        ): PerformanceManager =
            PerformanceManager(
                performance = performance,
                member = member,
                role = PerformanceRole.MANAGER,
            )
    }
}
