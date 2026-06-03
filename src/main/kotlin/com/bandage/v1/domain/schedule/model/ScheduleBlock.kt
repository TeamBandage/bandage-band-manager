package com.bandage.v1.domain.schedule.model

import com.bandage.v1.global.common.domain.BaseEntity
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

/**
 * 시간표 블록(하나의 연습 시간 구간).
 *
 * PRD-2 변경점:
 * - 단일 songId 제거 → SetlistTrack 과 N:M([ScheduleBlockTrack])
 * - 반복 배치 규칙(recurrenceRule) 및 배치 출처(placementOrigin) 메타데이터 추가
 */
@Entity
@Table(name = "p_schedule_block")
@SQLRestriction("deleted_at IS NULL")
open class ScheduleBlock(
    id: UUID,
    board: ScheduleBoard,
    date: LocalDate,
    startSlot: Int,
    durationSlots: Int,
    paletteIndex: Int?,
    titleOverride: String?,
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

    @Column(name = "block_date", nullable = false)
    var date: LocalDate = date
        protected set

    @Column(name = "start_slot", nullable = false)
    var startSlot: Int = startSlot
        protected set

    @Column(name = "duration_slots", nullable = false)
    var durationSlots: Int = durationSlots
        protected set

    @Column(name = "pinned", nullable = false)
    var pinned: Boolean = false
        protected set

    @Column(name = "palette_index", nullable = true)
    var paletteIndex: Int? = paletteIndex
        protected set

    @Column(name = "title_override", nullable = true)
    var titleOverride: String? = titleOverride
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
            date: LocalDate,
            startSlot: Int,
            durationSlots: Int,
            paletteIndex: Int? = null,
            titleOverride: String? = null,
            note: String? = null,
            recurrenceRule: RecurrenceRule = RecurrenceRule.none(),
            placementOrigin: PlacementOrigin = PlacementOrigin.MANUAL,
            id: UUID = UuidCreator.getTimeOrderedEpoch(),
        ): ScheduleBlock {
            validateSlot(startSlot, durationSlots)
            return ScheduleBlock(
                id = id,
                board = board,
                date = date,
                startSlot = startSlot,
                durationSlots = durationSlots,
                paletteIndex = paletteIndex,
                titleOverride = titleOverride,
                note = note,
                recurrenceRule = recurrenceRule,
                placementOrigin = placementOrigin,
            )
        }

        private fun validateSlot(
            startSlot: Int,
            durationSlots: Int,
        ) {
            require(startSlot in 0 until SLOTS_PER_DAY) {
                "startSlot must be in 0..${SLOTS_PER_DAY - 1}, was $startSlot"
            }
            require(durationSlots >= 1) {
                "durationSlots must be >= 1, was $durationSlots"
            }
            require(startSlot + durationSlots <= SLOTS_PER_DAY) {
                "startSlot + durationSlots must be <= $SLOTS_PER_DAY (got ${startSlot + durationSlots})"
            }
        }
    }

    fun reposition(
        date: LocalDate,
        startSlot: Int,
        durationSlots: Int,
    ) {
        validateSlot(startSlot, durationSlots)
        this.date = date
        this.startSlot = startSlot
        this.durationSlots = durationSlots
    }

    fun pin() {
        this.pinned = true
    }

    fun unpin() {
        this.pinned = false
    }

    fun updatePaletteIndex(index: Int?) {
        this.paletteIndex = index
    }

    fun updateTitleOverride(title: String?) {
        this.titleOverride = title
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
