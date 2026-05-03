package com.bandage.v1.domain.schedule.model

import com.bandage.v1.global.common.domain.BaseEntity
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapKeyColumn
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "p_member_schedule")
@IdClass(MemberScheduleId::class)
@SQLRestriction("deleted_at IS NULL")
open class MemberSchedule(
    meetingId: UUID,
    userId: Long,
    note: String?,
) : BaseEntity() {
    @Id
    @Column(name = "meeting_id", nullable = false)
    val meetingId: UUID = meetingId

    @Id
    @Column(name = "user_id", nullable = false)
    val userId: Long = userId

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "p_member_schedule_available_dates",
        joinColumns = [
            JoinColumn(name = "meeting_id", referencedColumnName = "meeting_id"),
            JoinColumn(name = "user_id", referencedColumnName = "user_id"),
        ],
    )
    @Column(name = "date_value", nullable = false)
    private var _availableDates: MutableSet<LocalDate> = mutableSetOf()
    val availableDates: Set<LocalDate> get() = _availableDates.toSet()

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "p_member_schedule_unavailable_dates",
        joinColumns = [
            JoinColumn(name = "meeting_id", referencedColumnName = "meeting_id"),
            JoinColumn(name = "user_id", referencedColumnName = "user_id"),
        ],
    )
    @Column(name = "date_value", nullable = false)
    private var _unavailableDates: MutableSet<LocalDate> = mutableSetOf()
    val unavailableDates: Set<LocalDate> get() = _unavailableDates.toSet()

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "p_member_schedule_blocks",
        joinColumns = [
            JoinColumn(name = "meeting_id", referencedColumnName = "meeting_id"),
            JoinColumn(name = "user_id", referencedColumnName = "user_id"),
        ],
    )
    @MapKeyColumn(name = "date_key")
    @Column(name = "block_hex", nullable = false, length = 12)
    private var _blocks: MutableMap<LocalDate, String> = mutableMapOf()
    val blocks: Map<LocalDate, String> get() = _blocks.toMap()

    @Column(name = "note", nullable = true, length = 500)
    var note: String? = note
        protected set

    @Column(name = "completed", nullable = false)
    var completed: Boolean = false
        protected set

    companion object {
        fun create(
            meetingId: UUID,
            userId: Long,
            note: String? = null,
        ): MemberSchedule =
            MemberSchedule(
                meetingId = meetingId,
                userId = userId,
                note = note,
            )
    }

    fun updateAvailability(
        availableDates: Set<LocalDate>?,
        unavailableDates: Set<LocalDate>?,
        blocks: Map<LocalDate, String>?,
    ) {
        availableDates?.let {
            _availableDates.clear()
            _availableDates.addAll(it)
        }
        unavailableDates?.let {
            _unavailableDates.clear()
            _unavailableDates.addAll(it)
        }
        blocks?.let {
            _blocks.clear()
            _blocks.putAll(it)
        }
    }

    fun updateNote(note: String?) {
        this.note = note
    }

    fun complete() {
        this.completed = true
    }

    fun reopen() {
        this.completed = false
    }
}
