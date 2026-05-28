package com.bandage.v1.domain.selection.repository

import com.bandage.v1.domain.selection.model.TrackSelection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TrackSelectionRepository :
    JpaRepository<TrackSelection, UUID>,
    TrackSelectionRepositoryCustom {
    fun existsByPerformanceIdAndLockedAtIsNull(performanceId: UUID): Boolean
}
