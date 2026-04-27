package com.bandage.v1.domain.setlist.model

import com.bandage.v1.domain.setlist.model.enums.MeetingPurpose
import com.bandage.v1.global.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.UuidGenerator
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "p_setlist_meeting")
@SQLRestriction("deleted_at IS NULL")
open class SetlistMeeting(
    bandId: UUID,
    title: String,
    purpose: MeetingPurpose,
    performanceId: UUID?,
    managerId: Long,
) : BaseEntity() {
    @Id
    @Column(name = "meeting_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @Column(name = "band_id", nullable = false)
    var bandId: UUID = bandId
        protected set

    @Column(name = "title", nullable = false)
    var title: String = title
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false)
    var purpose: MeetingPurpose = purpose
        protected set

    @Column(name = "performance_id", nullable = true)
    var performanceId: UUID? = performanceId
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
            bandId: UUID,
            title: String,
            purpose: MeetingPurpose,
            performanceId: UUID?,
            managerId: Long,
        ): SetlistMeeting =
            SetlistMeeting(
                bandId = bandId,
                title = title,
                purpose = purpose,
                performanceId = performanceId,
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
