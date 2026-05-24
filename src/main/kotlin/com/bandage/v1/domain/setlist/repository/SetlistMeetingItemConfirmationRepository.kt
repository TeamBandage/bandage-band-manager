package com.bandage.v1.domain.setlist.repository

import com.bandage.v1.domain.setlist.model.SetlistMeetingItem
import com.bandage.v1.domain.setlist.model.SetlistMeetingItemConfirmation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SetlistMeetingItemConfirmationRepository : JpaRepository<SetlistMeetingItemConfirmation, UUID> {
    fun findAllByItem(item: SetlistMeetingItem): List<SetlistMeetingItemConfirmation>

    fun findAllByItemIn(items: List<SetlistMeetingItem>): List<SetlistMeetingItemConfirmation>

    fun findByItemAndSessionIdAndMemberId(
        item: SetlistMeetingItem,
        sessionId: String,
        memberId: Long,
    ): SetlistMeetingItemConfirmation?

    fun countByItemAndSessionId(
        item: SetlistMeetingItem,
        sessionId: String,
    ): Long

    fun deleteAllByItemInAndMemberId(
        items: List<SetlistMeetingItem>,
        memberId: Long,
    )
}
