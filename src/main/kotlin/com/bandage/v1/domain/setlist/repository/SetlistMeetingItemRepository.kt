package com.bandage.v1.domain.setlist.repository

import com.bandage.v1.domain.setlist.model.SetlistMeeting
import com.bandage.v1.domain.setlist.model.SetlistMeetingItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SetlistMeetingItemRepository :
    JpaRepository<SetlistMeetingItem, UUID>,
    SetlistMeetingItemRepositoryCustom {
    fun findAllByMeeting(meeting: SetlistMeeting): List<SetlistMeetingItem>
}
