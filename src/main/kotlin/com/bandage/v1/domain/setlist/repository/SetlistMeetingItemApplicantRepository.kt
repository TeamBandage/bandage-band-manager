package com.bandage.v1.domain.setlist.repository

import com.bandage.v1.domain.setlist.model.SetlistMeetingItem
import com.bandage.v1.domain.setlist.model.SetlistMeetingItemApplicant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SetlistMeetingItemApplicantRepository : JpaRepository<SetlistMeetingItemApplicant, UUID> {
    fun findAllByItem(item: SetlistMeetingItem): List<SetlistMeetingItemApplicant>

    fun findAllByItemIn(items: List<SetlistMeetingItem>): List<SetlistMeetingItemApplicant>

    fun findByItemAndSessionIdAndMemberId(
        item: SetlistMeetingItem,
        sessionId: String,
        memberId: Long,
    ): SetlistMeetingItemApplicant?

    fun existsByItemAndSessionIdAndMemberId(
        item: SetlistMeetingItem,
        sessionId: String,
        memberId: Long,
    ): Boolean

    fun deleteAllByItemInAndMemberId(
        items: List<SetlistMeetingItem>,
        memberId: Long,
    )
}
