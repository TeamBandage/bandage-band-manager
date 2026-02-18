package com.bandage.v1.domain.practice.model

import com.bandage.v1.global.domain.BaseEntity
import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import java.util.*

@Entity
@Table(name = "p_practice_participant")
class PracticeParticipant(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practice_id")
    val practice: Practice,
    @Column(name = "member_id")
    val member: UUID
): BaseEntity() {
    @Id
    @Column(name = "practice_participant_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    var id: UUID? = null
}