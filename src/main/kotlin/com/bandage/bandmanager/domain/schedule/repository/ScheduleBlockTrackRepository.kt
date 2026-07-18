package com.bandage.bandmanager.domain.schedule.repository

import com.bandage.bandmanager.domain.schedule.model.ScheduleBlockTrack
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ScheduleBlockTrackRepository : JpaRepository<ScheduleBlockTrack, UUID> {
    fun findAllByBlockIdOrderByOrdinalAsc(blockId: UUID): List<ScheduleBlockTrack>

    fun findAllByBlockIdIn(blockIds: Collection<UUID>): List<ScheduleBlockTrack>

    fun deleteAllByBlockId(blockId: UUID)
}
