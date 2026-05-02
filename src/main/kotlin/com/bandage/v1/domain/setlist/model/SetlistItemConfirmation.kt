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
    name = "p_setlist_item_confirmation",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_setlist_item_confirmation",
            columnNames = ["setlist_item_id", "session_id", "member_id"],
        ),
    ],
)
@SQLRestriction("deleted_at IS NULL")
open class SetlistItemConfirmation(
    item: SetlistItem,
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
    @JoinColumn(name = "setlist_item_id", nullable = false)
    val item: SetlistItem = item

    @Column(name = "session_id", nullable = false)
    val sessionId: String = sessionId

    @Column(name = "member_id", nullable = false)
    val memberId: Long = memberId

    @Column(name = "confirmed_by", nullable = false)
    val confirmedBy: Long = confirmedBy

    companion object {
        fun create(
            item: SetlistItem,
            sessionId: String,
            memberId: Long,
            confirmedBy: Long,
        ): SetlistItemConfirmation =
            SetlistItemConfirmation(
                item = item,
                sessionId = sessionId,
                memberId = memberId,
                confirmedBy = confirmedBy,
            )
    }
}
