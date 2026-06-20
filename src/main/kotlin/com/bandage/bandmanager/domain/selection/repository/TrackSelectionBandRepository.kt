package com.bandage.bandmanager.domain.selection.repository

import com.bandage.bandmanager.domain.selection.model.TrackSelection
import com.bandage.bandmanager.domain.selection.model.TrackSelectionBand
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TrackSelectionBandRepository : JpaRepository<TrackSelectionBand, UUID> {
    fun findAllBySelection(selection: TrackSelection): List<TrackSelectionBand>

    fun findAllBySelectionIn(selections: Collection<TrackSelection>): List<TrackSelectionBand>

    fun findAllBySelectionId(selectionId: UUID): List<TrackSelectionBand>

    fun existsBySelectionAndBandId(
        selection: TrackSelection,
        bandId: UUID,
    ): Boolean
}
