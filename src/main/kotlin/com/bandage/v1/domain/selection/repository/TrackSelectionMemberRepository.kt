package com.bandage.v1.domain.selection.repository

import com.bandage.v1.domain.selection.model.TrackSelection
import com.bandage.v1.domain.selection.model.TrackSelectionMember
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TrackSelectionMemberRepository : JpaRepository<TrackSelectionMember, UUID> {
    fun findAllBySelection(selection: TrackSelection): List<TrackSelectionMember>

    fun findAllBySelectionId(selectionId: UUID): List<TrackSelectionMember>

    fun existsBySelectionAndMemberId(
        selection: TrackSelection,
        memberId: Long,
    ): Boolean

    fun deleteBySelection(selection: TrackSelection)

    fun findBySelectionAndMemberId(
        selection: TrackSelection,
        memberId: Long,
    ): TrackSelectionMember?
}
