package com.bandage.v1.domain.practice.model

import com.bandage.v1.global.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(name = "p_practice_participant")
@SQLRestriction("deleted_at IS NULL")
open class PracticeParticipant(
    practice: Practice,
    member: Long,
) : BaseEntity() {
    @Id
    @Column(name = "practice_participant_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practice_id")
    val practice: Practice = practice

    @Column(name = "member_id")
    val member: Long = member

    companion object {
        fun create(
            practice: Practice,
            member: Long,
        ): PracticeParticipant =
            PracticeParticipant(
                practice = practice,
                member = member,
            )
    }
}
