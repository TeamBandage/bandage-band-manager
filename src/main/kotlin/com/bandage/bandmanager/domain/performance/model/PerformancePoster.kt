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
    imageKey: String,
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

    @Column(name = "image_key", nullable = false)
    val imageKey: String = imageKey

    @Column(name = "description")
    var description: String? = description
        protected set

    companion object {
        fun create(
            performance: Performance,
            imageKey: String,
            description: String?,
        ): PerformancePoster =
            PerformancePoster(
                performance = performance,
                imageKey = imageKey,
                description = description,
            )
    }

    fun updateDescription(newDescription: String?) {
        this.description = newDescription
    }
}
