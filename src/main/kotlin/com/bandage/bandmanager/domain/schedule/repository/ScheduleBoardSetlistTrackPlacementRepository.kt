package com.bandage.bandmanager.domain.schedule.repository

import com.bandage.bandmanager.domain.schedule.model.ScheduleBoardSetlistTrackPlacement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ScheduleBoardSetlistTrackPlacementRepository : JpaRepository<ScheduleBoardSetlistTrackPlacement, UUID> {
    fun findAllByBoardId(boardId: UUID): List<ScheduleBoardSetlistTrackPlacement>

    fun deleteAllByBoardId(boardId: UUID)
}
