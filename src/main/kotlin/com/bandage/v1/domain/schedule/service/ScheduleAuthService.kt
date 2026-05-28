package com.bandage.v1.domain.schedule.service

import com.bandage.v1.domain.selection.repository.TrackSelectionMemberRepository
import com.bandage.v1.domain.selection.repository.TrackSelectionRepository
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ScheduleAuthService(
    private val trackSelectionRepository: TrackSelectionRepository,
    private val trackSelectionMemberRepository: TrackSelectionMemberRepository,
) {
    fun isParticipant(
        meetingId: UUID,
        memberId: Long,
    ): Boolean {
        val meeting =
            trackSelectionRepository.findByIdOrNull(meetingId)
                ?: throw BusinessException(ErrorCode.SETLIST_MEETING_NOT_FOUND)
        if (meeting.managerId == memberId) return true
        return trackSelectionMemberRepository.existsBySelectionAndMemberId(meeting, memberId)
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
            trackSelectionRepository.findByIdOrNull(meetingId)
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
