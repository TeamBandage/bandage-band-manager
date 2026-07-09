package com.bandage.bandmanager.domain.selection.repository

import com.bandage.bandmanager.domain.selection.model.TrackSelectionItem
import com.bandage.bandmanager.domain.selection.model.TrackSelectionItemConfirmation
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

    fun existsByItemAndSessionId(
        item: TrackSelectionItem,
        sessionId: String,
    ): Boolean

    fun deleteAllByItemInAndMemberId(
        items: List<TrackSelectionItem>,
        memberId: Long,
    )
}
