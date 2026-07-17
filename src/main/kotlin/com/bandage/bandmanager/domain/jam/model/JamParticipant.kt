package com.bandage.bandmanager.domain.jam.model

import com.bandage.bandmanager.global.common.domain.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(
    name = "p_jam_participant",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_jam_participant",
            columnNames = ["jam_id", "member_id"],
        ),
    ],
)
@SQLRestriction("deleted_at IS NULL")
open class JamParticipant(
    jam: Jam,
    member: Long,
) : BaseEntity() {
    @Id
    @Column(name = "jam_participant_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jam_id")
    val jam: Jam = jam

    @Column(name = "member_id")
    val member: Long = member

    @OneToMany(mappedBy = "jamParticipant", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    private var _sessions: MutableList<JamParticipantSession> = mutableListOf()

    val sessions: List<JamParticipantSession> get() = _sessions.toList()

    fun assignSession(sessionId: String) {
        _sessions.add(JamParticipantSession.create(jamParticipant = this, sessionId = sessionId))
    }

    fun unassignSession(sessionId: String) {
        _sessions.removeIf { it.sessionId == sessionId }
    }

    companion object {
        fun create(
            jam: Jam,
            member: Long,
        ): JamParticipant =
            JamParticipant(
                jam = jam,
                member = member,
            )
    }
}
