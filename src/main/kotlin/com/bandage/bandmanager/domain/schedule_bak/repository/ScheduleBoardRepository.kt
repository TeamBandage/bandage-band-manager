package com.bandage.bandmanager.domain.schedule_bak.repository

import com.bandage.bandmanager.domain.schedule_bak.model.ScheduleBoard
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ScheduleBoardRepository : JpaRepository<ScheduleBoard, UUID> {
    fun findAllByPerformanceId(performanceId: UUID): List<ScheduleBoard>

    fun findFirstByPerformanceIdAndConfirmedTrue(performanceId: UUID): ScheduleBoard?

    // 마이그레이션 기간 한정(구 meeting 스코프). 추후 제거 예정.
    fun findAllByMeetingId(meetingId: UUID): List<ScheduleBoard>
}
