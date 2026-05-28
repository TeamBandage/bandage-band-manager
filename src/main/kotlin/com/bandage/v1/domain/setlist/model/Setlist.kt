package com.bandage.v1.domain.setlist.model

import com.bandage.v1.global.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(name = "p_setlist")
@SQLRestriction("deleted_at IS NULL")
open class Setlist(
    trackSelectionId: UUID,
    title: String,
    managerId: Long,
) : BaseEntity() {
    @Id
    @Column(name = "setlist_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @Column(name = "track_selection_id", nullable = false)
    var trackSelectionId: UUID = trackSelectionId
        protected set

    @Column(name = "title", nullable = false)
    var title: String = title
        protected set

    @Column(name = "manager_id", nullable = false)
    var managerId: Long = managerId
        protected set

    companion object {
        fun create(
            trackSelectionId: UUID,
            title: String,
            managerId: Long,
        ): Setlist =
            Setlist(
                trackSelectionId = trackSelectionId,
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
}
