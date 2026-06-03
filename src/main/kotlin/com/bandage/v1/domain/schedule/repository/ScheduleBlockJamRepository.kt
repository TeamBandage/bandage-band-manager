package com.bandage.v1.domain.schedule.repository

import com.bandage.v1.domain.schedule.model.ScheduleBlockJam
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ScheduleBlockJamRepository : JpaRepository<ScheduleBlockJam, UUID> {
    fun findAllByScheduleBoardId(scheduleBoardId: UUID): List<ScheduleBlockJam>

    fun findAllByScheduleBlockId(scheduleBlockId: UUID): List<ScheduleBlockJam>

    fun deleteAllByScheduleBoardId(scheduleBoardId: UUID)
}
