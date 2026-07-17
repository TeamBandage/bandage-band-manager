package com.bandage.bandmanager.domain.jam.model

import com.bandage.bandmanager.global.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(
    name = "p_jam_participant_session",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_jam_participant_session",
            columnNames = ["jam_participant_id", "session_id"],
        ),
    ],
)
open class JamParticipantSession(
    jamParticipant: JamParticipant,
    sessionId: String,
) : BaseEntity() {
    @Id
    @Column(name = "jam_participant_session_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jam_participant_id")
    val jamParticipant: JamParticipant = jamParticipant

    @Column(name = "session_id", nullable = false)
    val sessionId: String = sessionId

    companion object {
        fun create(
            jamParticipant: JamParticipant,
            sessionId: String,
        ): JamParticipantSession =
            JamParticipantSession(
                jamParticipant = jamParticipant,
                sessionId = sessionId,
            )
    }
}
