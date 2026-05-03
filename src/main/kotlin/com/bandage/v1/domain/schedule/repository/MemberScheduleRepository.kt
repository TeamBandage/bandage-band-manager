package com.bandage.v1.domain.schedule.repository

import com.bandage.v1.domain.schedule.model.MemberSchedule
import com.bandage.v1.domain.schedule.model.MemberScheduleId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface MemberScheduleRepository : JpaRepository<MemberSchedule, MemberScheduleId> {
    fun findByMeetingIdAndUserId(
        meetingId: UUID,
        userId: Long,
    ): MemberSchedule?

    fun findAllByMeetingId(meetingId: UUID): List<MemberSchedule>
}
