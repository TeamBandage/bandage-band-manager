package com.bandage.bandmanager.domain.selection.repository

import com.bandage.bandmanager.domain.selection.model.TrackSelectionItem
import com.bandage.bandmanager.domain.selection.model.TrackSelectionItemApplicant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TrackSelectionItemApplicantRepository : JpaRepository<TrackSelectionItemApplicant, UUID> {
    fun findAllByItem(item: TrackSelectionItem): List<TrackSelectionItemApplicant>

    fun findAllByItemIn(items: List<TrackSelectionItem>): List<TrackSelectionItemApplicant>

    fun findByItemAndSessionIdAndMemberId(
        item: TrackSelectionItem,
        sessionId: String,
        memberId: Long,
    ): TrackSelectionItemApplicant?

    fun existsByItemAndSessionIdAndMemberId(
        item: TrackSelectionItem,
        sessionId: String,
        memberId: Long,
    ): Boolean

    fun deleteAllByItemInAndMemberId(
        items: List<TrackSelectionItem>,
        memberId: Long,
    )
}
