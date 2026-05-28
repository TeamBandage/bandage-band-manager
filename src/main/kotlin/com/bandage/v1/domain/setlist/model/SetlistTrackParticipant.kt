package com.bandage.v1.domain.setlist.model

import com.bandage.v1.global.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(
    name = "p_setlist_track_participant",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_setlist_track_participant",
            columnNames = ["setlist_track_id", "session_id", "member_id"],
        ),
    ],
)
@SQLRestriction("deleted_at IS NULL")
open class SetlistTrackParticipant(
    track: SetlistTrack,
    sessionId: String,
    memberId: Long,
) : BaseEntity() {
    @Id
    @Column(name = "participant_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setlist_track_id", nullable = false)
    val track: SetlistTrack = track

    @Column(name = "session_id", nullable = false)
    val sessionId: String = sessionId

    @Column(name = "member_id", nullable = false)
    val memberId: Long = memberId

    companion object {
        fun create(
            track: SetlistTrack,
            sessionId: String,
            memberId: Long,
        ): SetlistTrackParticipant =
            SetlistTrackParticipant(
                track = track,
                sessionId = sessionId,
                memberId = memberId,
            )
    }
}
