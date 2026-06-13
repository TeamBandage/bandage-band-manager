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
    name = "p_track_selection_item_confirmation",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_track_selection_item_confirmation",
            columnNames = ["track_selection_item_id", "session_id", "member_id"],
        ),
    ],
)
@SQLRestriction("deleted_at IS NULL")
open class TrackSelectionItemConfirmation(
    item: TrackSelectionItem,
    sessionId: String,
    memberId: Long,
    confirmedBy: Long,
) : BaseEntity() {
    @Id
    @Column(name = "confirmation_id")
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

    @Column(name = "confirmed_by", nullable = false)
    val confirmedBy: Long = confirmedBy

    companion object {
        fun create(
            item: TrackSelectionItem,
            sessionId: String,
            memberId: Long,
            confirmedBy: Long,
        ): TrackSelectionItemConfirmation =
            TrackSelectionItemConfirmation(
                item = item,
                sessionId = sessionId,
                memberId = memberId,
                confirmedBy = confirmedBy,
            )
    }
}
