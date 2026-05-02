package com.bandage.v1.domain.setlist.repository

import com.bandage.v1.domain.setlist.model.SetlistMeeting
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SetlistMeetingRepository :
    JpaRepository<SetlistMeeting, UUID>,
    SetlistMeetingRepositoryCustom {
    fun existsByPerformanceIdAndLockedAtIsNull(performanceId: UUID): Boolean
}
