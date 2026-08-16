package com.bandage.bandmanager.domain.schedule.repository

import com.bandage.bandmanager.domain.schedule.model.ScheduleBoardSetlistItemPlacement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ScheduleBoardSetlistItemPlacementRepository : JpaRepository<ScheduleBoardSetlistItemPlacement, UUID> {
    fun findAllByBoardId(boardId: UUID): List<ScheduleBoardSetlistItemPlacement>

    fun deleteAllByBoardId(boardId: UUID)
}
