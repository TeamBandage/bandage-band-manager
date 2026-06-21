package com.bandage.bandmanager.domain.performance.model

import com.bandage.bandmanager.global.common.domain.BaseEntity
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
@Table(name = "p_performance_poster")
open class PerformancePoster(
    performance: Performance,
    s3Url: String,
    description: String?,
) : BaseEntity() {
    @Id
    @Column(name = "performance_poster_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id", nullable = false)
    val performance: Performance = performance

    @Column(name = "s3_url", nullable = false)
    val s3Url: String = s3Url

    @Column(name = "description")
    val description: String? = description

    companion object {
        fun create(
            performance: Performance,
            s3Url: String,
            description: String?,
        ): PerformancePoster =
            PerformancePoster(
                performance = performance,
                s3Url = s3Url,
                description = description,
            )
    }
}
