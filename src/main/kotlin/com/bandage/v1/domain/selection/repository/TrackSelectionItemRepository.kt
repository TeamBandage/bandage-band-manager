package com.bandage.v1.domain.selection.repository

import com.bandage.v1.domain.selection.model.TrackSelection
import com.bandage.v1.domain.selection.model.TrackSelectionItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TrackSelectionItemRepository :
    JpaRepository<TrackSelectionItem, UUID>,
    TrackSelectionItemRepositoryCustom {
    fun findAllBySelection(selection: TrackSelection): List<TrackSelectionItem>

    fun findAllBySelectionAndIsSelectedTrue(selection: TrackSelection): List<TrackSelectionItem>
}
