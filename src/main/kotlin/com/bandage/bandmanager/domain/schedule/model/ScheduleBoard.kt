package com.bandage.bandmanager.domain.schedule.model

import com.bandage.bandmanager.global.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.UuidGenerator
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "p_schedule_board")
@SQLRestriction("deleted_at IS NULL")
open class ScheduleBoard(
    setlistId: UUID,
    name: String,
    boardTimeRangeFrom: Int,
    boardTimeRangeTo: Int,
    windowFrom: LocalDate?,
    windowTo: LocalDate?,
) : BaseEntity() {
    @Id
    @Column(name = "schedule_board_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @Column(name = "setlist_id", nullable = false)
    val setlistId: UUID = setlistId

    @Column(name = "name", nullable = false, length = 50)
    var name: String = name
        protected set

    @Column(name = "confirmed", nullable = false)
    var confirmed: Boolean = false
        protected set

    /**
     * 보드 전체에 적용되는 하루 안의 배치 가능 시간대. [from, to) 반열린 구간이며 값은 **슬롯 인덱스**다.
     *
     * 하루는 30분 단위 48슬롯이므로 from 은 0..47, to 는 1..48 이다.
     * (예: from = 18 → 09:00, to = 44 → 22:00, to = 48 → 24:00)
     * 시(hour)가 아니라 슬롯 인덱스임에 주의한다.
     *
     * Slot, WeeklyRule, AvailabilityException 과 동일한 슬롯 규약을 쓴다.
     */
    @Column(name = "board_time_range_from", nullable = false)
    var boardTimeRangeFrom: Int = boardTimeRangeFrom
        protected set

    @Column(name = "board_time_range_to", nullable = false)
    var boardTimeRangeTo: Int = boardTimeRangeTo
        protected set

    @Column(name = "window_from", nullable = true)
    var windowFrom: LocalDate? = windowFrom
        protected set

    @Column(name = "window_to", nullable = true)
    var windowTo: LocalDate? = windowTo
        protected set

    companion object {
        /** 기본 배치 가능 시간대: 슬롯 18(09:00) ~ 44(22:00). */
        const val DEFAULT_BOARD_TIME_RANGE_FROM = 18
        const val DEFAULT_BOARD_TIME_RANGE_TO = 44

        fun create(
            setlistId: UUID,
            name: String,
            boardTimeRangeFrom: Int = DEFAULT_BOARD_TIME_RANGE_FROM,
            boardTimeRangeTo: Int = DEFAULT_BOARD_TIME_RANGE_TO,
            windowFrom: LocalDate? = null,
            windowTo: LocalDate? = null,
        ): ScheduleBoard {
            validateTimeRange(boardTimeRangeFrom, boardTimeRangeTo)
            return ScheduleBoard(
                setlistId = setlistId,
                name = name,
                boardTimeRangeFrom = boardTimeRangeFrom,
                boardTimeRangeTo = boardTimeRangeTo,
                windowFrom = windowFrom,
                windowTo = windowTo,
            )
        }

        fun validateTimeRange(
            from: Int,
            to: Int,
        ) {
            require(from in 0 until Slot.SLOTS_PER_DAY) {
                "boardTimeRangeFrom must be in 0..${Slot.SLOTS_PER_DAY - 1}, was $from"
            }
            require(to in 1..Slot.SLOTS_PER_DAY) {
                "boardTimeRangeTo must be in 1..${Slot.SLOTS_PER_DAY}, was $to"
            }
            require(from < to) {
                "boardTimeRangeFrom($from) must be less than boardTimeRangeTo($to)"
            }
        }
    }

    fun scheduleWindowOrNull(): ScheduleWindow? {
        val from = windowFrom
        val to = windowTo
        return if (from != null && to != null) ScheduleWindow(from, to) else null
    }

    fun updateWindow(
        from: LocalDate?,
        to: LocalDate?,
    ) {
        this.windowFrom = from
        this.windowTo = to
    }

    fun updateTimeRange(
        from: Int,
        to: Int,
    ) {
        validateTimeRange(from, to)
        this.boardTimeRangeFrom = from
        this.boardTimeRangeTo = to
    }

    fun rename(newName: String) {
        this.name = newName
    }

    fun confirm() {
        this.confirmed = true
    }

    fun unconfirm() {
        this.confirmed = false
    }
}
