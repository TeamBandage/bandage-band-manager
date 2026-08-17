package com.bandage.bandmanager.domain.schedule.model

import com.bandage.bandmanager.domain.schedule.model.enums.PlacementOrigin
import com.bandage.bandmanager.global.common.domain.BaseEntity
import com.github.f4b6a3.uuid.UuidCreator
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.hibernate.annotations.SQLRestriction
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "p_schedule_block")
@SQLRestriction("deleted_at IS NULL")
open class ScheduleBlock(
    id: UUID,
    board: ScheduleBoard,
    slot: Slot,
    title: String?,
    note: String?,
    placementOrigin: PlacementOrigin,
) : BaseEntity() {
    @Id
    @Column(name = "schedule_block_id")
    val id: UUID = id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_board_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val board: ScheduleBoard = board

    @Embedded
    var slot: Slot = slot
        protected set

    val startDate: LocalDate get() = slot.startDate
    val endDate: LocalDate get() = slot.endDate
    val startSlot: Int get() = slot.startSlot
    val endSlot: Int get() = slot.endSlot

    @Column(name = "pinned", nullable = false)
    var pinned: Boolean = false
        protected set

    @Column(name = "title", nullable = true)
    var title: String? = title
        protected set

    @Column(name = "note", nullable = true, length = 200)
    var note: String? = note
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "placement_origin", nullable = false)
    var placementOrigin: PlacementOrigin = placementOrigin
        protected set

    companion object {
        fun create(
            board: ScheduleBoard,
            slot: Slot,
            title: String? = null,
            note: String? = null,
            placementOrigin: PlacementOrigin = PlacementOrigin.MANUAL,
            id: UUID = UuidCreator.getTimeOrderedEpoch(),
        ): ScheduleBlock =
            ScheduleBlock(
                id = id,
                board = board,
                slot = slot,
                title = title,
                note = note,
                placementOrigin = placementOrigin,
            )
    }

    fun reposition(slot: Slot) {
        this.slot = slot
    }

    fun pin() {
        this.pinned = true
    }

    fun unpin() {
        this.pinned = false
    }

    fun updateTitle(title: String?) {
        this.title = title
    }

    fun updateNote(note: String?) {
        this.note = note
    }

    fun updatePlacementOrigin(origin: PlacementOrigin) {
        this.placementOrigin = origin
    }
}
