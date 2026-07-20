package com.bandage.bandmanager.domain.schedule.model

import com.bandage.bandmanager.global.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Embedded
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
    constraints: ScheduleBoardConstraints,
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

    @Embedded
    val constraints: ScheduleBoardConstraints = constraints

    @Column(name = "window_from", nullable = true)
    var windowFrom: LocalDate? = windowFrom
        protected set

    @Column(name = "window_to", nullable = true)
    var windowTo: LocalDate? = windowTo
        protected set

    companion object {
        fun create(
            setlistId: UUID,
            name: String,
            constraints: ScheduleBoardConstraints,
            windowFrom: LocalDate? = null,
            windowTo: LocalDate? = null,
        ): ScheduleBoard =
            ScheduleBoard(
                setlistId = setlistId,
                name = name,
                constraints = constraints,
                windowFrom = windowFrom,
                windowTo = windowTo,
            )
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
