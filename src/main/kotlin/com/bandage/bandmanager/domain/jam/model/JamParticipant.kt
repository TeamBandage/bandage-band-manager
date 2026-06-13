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
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(
    name = "p_jam_participant",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_jam_participant",
            columnNames = ["jam_id", "session_id", "member_id"],
        ),
    ],
)
@SQLRestriction("deleted_at IS NULL")
open class JamParticipant(
    jam: Jam,
    sessionId: String,
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

    @Column(name = "session_id", nullable = false)
    val sessionId: String = sessionId

    @Column(name = "member_id")
    val member: Long = member

    companion object {
        fun create(
            jam: Jam,
            sessionId: String,
            member: Long,
        ): JamParticipant =
            JamParticipant(
                jam = jam,
                sessionId = sessionId,
                member = member,
            )
    }
}
