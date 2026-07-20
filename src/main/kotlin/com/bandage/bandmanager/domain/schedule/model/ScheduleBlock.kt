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
import java.time.temporal.ChronoUnit
import java.util.UUID

@Entity
@Table(name = "p_schedule_block")
@SQLRestriction("deleted_at IS NULL")
open class ScheduleBlock(
    id: UUID,
    board: ScheduleBoard,
    startDate: LocalDate,
    endDate: LocalDate,
    startSlot: Int,
    endSlot: Int,
    title: String?,
    note: String?,
    recurrenceRule: RecurrenceRule,
    placementOrigin: PlacementOrigin,
) : BaseEntity() {
    @Id
    @Column(name = "schedule_block_id")
    val id: UUID = id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_board_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val board: ScheduleBoard = board

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate = startDate
        protected set

    @Column(name = "end_date", nullable = false)
    var endDate: LocalDate = endDate
        protected set

    @Column(name = "start_slot", nullable = false)
    var startSlot: Int = startSlot
        protected set

    @Column(name = "end_slot", nullable = false)
    var endSlot: Int = endSlot
        protected set

    @Column(name = "pinned", nullable = false)
    var pinned: Boolean = false
        protected set

    @Column(name = "title", nullable = true)
    var title: String? = title
        protected set

    @Column(name = "note", nullable = true, length = 200)
    var note: String? = note
        protected set

    @Embedded
    var recurrenceRule: RecurrenceRule = recurrenceRule
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "placement_origin", nullable = false)
    var placementOrigin: PlacementOrigin = placementOrigin
        protected set

    companion object {
        const val SLOTS_PER_DAY = 48

        fun create(
            board: ScheduleBoard,
            startDate: LocalDate,
            endDate: LocalDate,
            startSlot: Int,
            endSlot: Int,
            title: String? = null,
            note: String? = null,
            recurrenceRule: RecurrenceRule = RecurrenceRule.none(),
            placementOrigin: PlacementOrigin = PlacementOrigin.MANUAL,
            id: UUID = UuidCreator.getTimeOrderedEpoch(),
        ): ScheduleBlock {
            validateSlot(startDate, endDate, startSlot, endSlot)
            return ScheduleBlock(
                id = id,
                board = board,
                startDate = startDate,
                endDate = endDate,
                startSlot = startSlot,
                endSlot = endSlot,
                title = title,
                note = note,
                recurrenceRule = recurrenceRule,
                placementOrigin = placementOrigin,
            )
        }

        /** [startDate, startSlot) 부터 [endDate, endSlot) 까지의 총 슬롯 수(exclusive). */
        fun totalSlots(
            startDate: LocalDate,
            endDate: LocalDate,
            startSlot: Int,
            endSlot: Int,
        ): Int {
            val dayDiff = ChronoUnit.DAYS.between(startDate, endDate).toInt()
            return dayDiff * SLOTS_PER_DAY + (endSlot - startSlot)
        }

        fun validateSlot(
            startDate: LocalDate,
            endDate: LocalDate,
            startSlot: Int,
            endSlot: Int,
        ) {
            require(!endDate.isBefore(startDate)) {
                "endDate must be on or after startDate (startDate=$startDate, endDate=$endDate)"
            }
            require(startSlot in 0 until SLOTS_PER_DAY) {
                "startSlot must be in 0..${SLOTS_PER_DAY - 1}, was $startSlot"
            }
            require(endSlot in 0 until SLOTS_PER_DAY) {
                "endSlot must be in 0..${SLOTS_PER_DAY - 1}, was $endSlot"
            }
            require(totalSlots(startDate, endDate, startSlot, endSlot) >= 1) {
                "end must be after start (startDate=$startDate, startSlot=$startSlot, endDate=$endDate, endSlot=$endSlot)"
            }
        }
    }

    fun reposition(
        startDate: LocalDate,
        endDate: LocalDate,
        startSlot: Int,
        endSlot: Int,
    ) {
        validateSlot(startDate, endDate, startSlot, endSlot)
        this.startDate = startDate
        this.endDate = endDate
        this.startSlot = startSlot
        this.endSlot = endSlot
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

    fun updateRecurrenceRule(rule: RecurrenceRule) {
        this.recurrenceRule = rule
    }

    fun updatePlacementOrigin(origin: PlacementOrigin) {
        this.placementOrigin = origin
    }
}
