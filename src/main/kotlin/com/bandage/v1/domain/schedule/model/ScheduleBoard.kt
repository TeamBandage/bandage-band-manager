package com.bandage.v1.domain.schedule.model

import com.bandage.v1.global.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(name = "p_schedule_board")
@SQLRestriction("deleted_at IS NULL")
open class ScheduleBoard(
    meetingId: UUID,
    name: String,
    paletteSeed: Int?,
    constraints: ScheduleBoardConstraints,
) : BaseEntity() {
    @Id
    @Column(name = "schedule_board_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @Column(name = "meeting_id", nullable = false)
    val meetingId: UUID = meetingId

    @Column(name = "name", nullable = false, length = 50)
    var name: String = name
        protected set

    @Column(name = "palette_seed", nullable = true)
    var paletteSeed: Int? = paletteSeed
        protected set

    @Column(name = "confirmed", nullable = false)
    var confirmed: Boolean = false
        protected set

    @Embedded
    val constraints: ScheduleBoardConstraints = constraints

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set

    companion object {
        fun create(
            meetingId: UUID,
            name: String,
            paletteSeed: Int? = null,
            constraints: ScheduleBoardConstraints = ScheduleBoardConstraints(),
        ): ScheduleBoard =
            ScheduleBoard(
                meetingId = meetingId,
                name = name,
                paletteSeed = paletteSeed,
                constraints = constraints,
            )
    }

    fun rename(newName: String) {
        this.name = newName
    }

    fun updatePaletteSeed(seed: Int?) {
        this.paletteSeed = seed
    }

    fun confirm() {
        this.confirmed = true
    }

    fun unconfirm() {
        this.confirmed = false
    }
}
