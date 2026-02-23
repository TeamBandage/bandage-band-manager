package com.bandage.v1.domain.practice.model

import com.bandage.v1.global.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(name = "p_practice_session")
class PracticeSession(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practice_id")
    val practice: Practice,
    @Column(name = "label")
    var label: String,
    @Column(name = "session_type", nullable = false)
    var type: SessionType = SessionType.ETC,
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id")
    var participant: PracticeParticipant? = null,
) : BaseEntity() {
    @Id
    @Column(name = "practice_session_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    var id: UUID? = null

    companion object {
        fun create(
            practice: Practice,
            label: String,
            type: SessionType,
        ): PracticeSession =
            PracticeSession(
                practice = practice,
                label = label,
                type = type,
            )
    }

    fun updateLabel(newLabel: String) {
        this.label = newLabel
    }

    fun assignParticipant(participant: PracticeParticipant) {
        this.participant = participant
    }

    fun withdrawParticipant() {
        this.participant = null
    }
}
