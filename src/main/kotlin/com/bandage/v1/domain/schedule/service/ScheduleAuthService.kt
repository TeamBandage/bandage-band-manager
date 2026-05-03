package com.bandage.v1.domain.schedule.service

import com.bandage.v1.domain.setlist.repository.SetlistMeetingMemberRepository
import com.bandage.v1.domain.setlist.repository.SetlistMeetingRepository
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ScheduleAuthService(
    private val setlistMeetingRepository: SetlistMeetingRepository,
    private val setlistMeetingMemberRepository: SetlistMeetingMemberRepository,
) {
    fun isParticipant(
        meetingId: UUID,
        memberId: Long,
    ): Boolean {
        val meeting =
            setlistMeetingRepository.findByIdOrNull(meetingId)
                ?: throw BusinessException(ErrorCode.SETLIST_MEETING_NOT_FOUND)
        if (meeting.managerId == memberId) return true
        return setlistMeetingMemberRepository.existsByMeetingAndMemberId(meeting, memberId)
    }

    fun isSelf(
        targetUserId: Long,
        memberId: Long,
    ): Boolean = targetUserId == memberId

    fun isManager(
        meetingId: UUID,
        memberId: Long,
    ): Boolean {
        val meeting =
            setlistMeetingRepository.findByIdOrNull(meetingId)
                ?: throw BusinessException(ErrorCode.SETLIST_MEETING_NOT_FOUND)
        return meeting.managerId == memberId
    }

    fun validateParticipant(
        meetingId: UUID,
        memberId: Long,
    ) {
        if (!isParticipant(meetingId, memberId)) {
            throw BusinessException(ErrorCode.SETLIST_MEETING_FORBIDDEN)
        }
    }

    fun validateManager(
        meetingId: UUID,
        memberId: Long,
    ) {
        if (!isManager(meetingId, memberId)) {
            throw BusinessException(ErrorCode.SETLIST_MEETING_NOT_MANAGER)
        }
    }
}
