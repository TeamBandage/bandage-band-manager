package com.bandage.v1.domain.setlist.repository

import com.bandage.v1.domain.setlist.model.SetlistMeeting
import com.bandage.v1.domain.setlist.model.SetlistMeetingMember
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SetlistMeetingMemberRepository : JpaRepository<SetlistMeetingMember, UUID> {
    fun findAllByMeeting(meeting: SetlistMeeting): List<SetlistMeetingMember>

    fun findAllByMeetingId(meetingId: UUID): List<SetlistMeetingMember>

    fun existsByMeetingAndMemberId(
        meeting: SetlistMeeting,
        memberId: Long,
    ): Boolean

    fun deleteByMeeting(meeting: SetlistMeeting)

    fun findByMeetingAndMemberId(
        meeting: SetlistMeeting,
        memberId: Long,
    ): SetlistMeetingMember?
}
