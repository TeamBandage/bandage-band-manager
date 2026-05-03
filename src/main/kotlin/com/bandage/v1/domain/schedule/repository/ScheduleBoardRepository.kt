package com.bandage.v1.domain.schedule.repository

import com.bandage.v1.domain.schedule.model.ScheduleBoard
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ScheduleBoardRepository : JpaRepository<ScheduleBoard, UUID> {
    fun findAllByMeetingId(meetingId: UUID): List<ScheduleBoard>

    fun findFirstByMeetingIdAndConfirmedTrue(meetingId: UUID): ScheduleBoard?
}
