package com.bandage.bandmanager.domain.selection.model

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
    name = "p_track_selection_item_applicant",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_track_selection_item_applicant",
            columnNames = ["track_selection_item_id", "session_id", "member_id"],
        ),
    ],
)
@SQLRestriction("deleted_at IS NULL")
open class TrackSelectionItemApplicant(
    item: TrackSelectionItem,
    sessionId: String,
    memberId: Long,
) : BaseEntity() {
    @Id
    @Column(name = "applicant_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_selection_item_id", nullable = false)
    val item: TrackSelectionItem = item

    @Column(name = "session_id", nullable = false)
    val sessionId: String = sessionId

    @Column(name = "member_id", nullable = false)
    val memberId: Long = memberId

    companion object {
        fun create(
            item: TrackSelectionItem,
            sessionId: String,
            memberId: Long,
        ): TrackSelectionItemApplicant = TrackSelectionItemApplicant(item = item, sessionId = sessionId, memberId = memberId)
    }
}
