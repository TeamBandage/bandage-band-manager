package com.bandage.v1.domain.practice.model

import com.bandage.v1.domain.practice.model.enums.SessionType
import com.bandage.v1.global.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(name = "p_practice_session")
@SQLRestriction("deleted_at IS NULL")
open class PracticeSession(
    practice: Practice,
    label: String,
    type: SessionType = SessionType.ETC,
    participant: PracticeParticipant?,
) : BaseEntity() {
    @Id
    @Column(name = "practice_session_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practice_id")
    val practice: Practice = practice

    @Column(name = "label")
    var label: String = label
        protected set

    @Column(name = "session_type", nullable = false)
    var type: SessionType = type
        protected set

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id")
    var participant: PracticeParticipant? = participant
        protected set

    companion object {
        fun create(
            practice: Practice,
            label: String,
            type: SessionType,
            participant: PracticeParticipant?,
        ): PracticeSession =
            PracticeSession(
                practice = practice,
                label = label,
                type = type,
                participant = participant,
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
