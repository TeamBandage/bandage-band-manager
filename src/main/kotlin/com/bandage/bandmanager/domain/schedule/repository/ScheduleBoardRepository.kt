package com.bandage.bandmanager.domain.schedule.repository

import com.bandage.bandmanager.domain.schedule.model.ScheduleBoard
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ScheduleBoardRepository : JpaRepository<ScheduleBoard, UUID> {
    fun findAllBySetlistId(setlistId: UUID): List<ScheduleBoard>

    fun findFirstBySetlistIdAndConfirmedTrue(setlistId: UUID): ScheduleBoard?
}
