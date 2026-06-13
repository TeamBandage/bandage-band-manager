package com.bandage.bandmanager.domain.schedule.repository

import com.bandage.bandmanager.domain.schedule.model.ScheduleBlock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ScheduleBlockRepository : JpaRepository<ScheduleBlock, UUID> {
    fun findAllByBoardId(boardId: UUID): List<ScheduleBlock>

    fun deleteAllByBoardId(boardId: UUID)
}
