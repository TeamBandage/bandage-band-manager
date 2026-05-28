package com.bandage.v1.domain.selection.model

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
@Table(name = "p_track_selection_item_chat_message")
@SQLRestriction("deleted_at IS NULL")
open class TrackSelectionItemChatMessage(
    item: TrackSelectionItem,
    memberId: Long,
    message: String,
) : BaseEntity() {
    @Id
    @Column(name = "message_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_selection_item_id", nullable = false)
    val item: TrackSelectionItem = item

    @Column(name = "member_id", nullable = false)
    val memberId: Long = memberId

    @Column(name = "message", nullable = false, length = 500)
    var message: String = message
        protected set

    companion object {
        fun create(
            item: TrackSelectionItem,
            memberId: Long,
            message: String,
        ): TrackSelectionItemChatMessage = TrackSelectionItemChatMessage(item = item, memberId = memberId, message = message)
    }
}
