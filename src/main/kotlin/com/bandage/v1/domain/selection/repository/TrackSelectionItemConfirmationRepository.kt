package com.bandage.v1.domain.selection.repository

import com.bandage.v1.domain.selection.model.TrackSelectionItem
import com.bandage.v1.domain.selection.model.TrackSelectionItemConfirmation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TrackSelectionItemConfirmationRepository : JpaRepository<TrackSelectionItemConfirmation, UUID> {
    fun findAllByItem(item: TrackSelectionItem): List<TrackSelectionItemConfirmation>

    fun findAllByItemIn(items: List<TrackSelectionItem>): List<TrackSelectionItemConfirmation>

    fun findByItemAndSessionIdAndMemberId(
        item: TrackSelectionItem,
        sessionId: String,
        memberId: Long,
    ): TrackSelectionItemConfirmation?

    fun countByItemAndSessionId(
        item: TrackSelectionItem,
        sessionId: String,
    ): Long

    fun deleteAllByItemInAndMemberId(
        items: List<TrackSelectionItem>,
        memberId: Long,
    )
}
