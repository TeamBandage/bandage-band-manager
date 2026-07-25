package com.bandage.bandmanager.domain.selection.model

import com.bandage.bandmanager.global.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.UuidGenerator
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "p_track_selection")
@SQLRestriction("deleted_at IS NULL")
open class TrackSelection(
    title: String,
    managerId: Long,
) : BaseEntity() {
    @Id
    @Column(name = "track_selection_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @Column(name = "title", nullable = false)
    var title: String = title
        protected set

    @Column(name = "manager_id", nullable = false)
    var managerId: Long = managerId
        protected set

    @Column(name = "locked_at", nullable = true)
    var lockedAt: LocalDateTime? = null
        protected set

    val isLocked: Boolean get() = lockedAt != null

    companion object {
        fun create(
            title: String,
            managerId: Long,
        ): TrackSelection =
            TrackSelection(
                title = title,
                managerId = managerId,
            )
    }

    fun updateTitle(newTitle: String) {
        this.title = newTitle
    }

    fun changeManager(newManagerId: Long) {
        this.managerId = newManagerId
    }

    fun lock() {
        this.lockedAt = LocalDateTime.now()
    }

    fun unlock() {
        this.lockedAt = null
    }
}
