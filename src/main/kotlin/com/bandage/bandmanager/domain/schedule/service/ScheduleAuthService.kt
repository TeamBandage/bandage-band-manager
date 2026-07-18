package com.bandage.bandmanager.domain.schedule.service

import com.bandage.bandmanager.domain.performance.model.Performance
import com.bandage.bandmanager.domain.performance.repository.PerformanceManagerRepository
import com.bandage.bandmanager.domain.performance.repository.PerformanceRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionMemberRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionRepository
import com.bandage.bandmanager.domain.setlist.repository.SetlistTrackParticipantRepository
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ScheduleAuthService(
    private val trackSelectionRepository: TrackSelectionRepository,
    private val trackSelectionMemberRepository: TrackSelectionMemberRepository,
    private val performanceRepository: PerformanceRepository,
    private val performanceManagerRepository: PerformanceManagerRepository,
    private val setlistTrackParticipantRepository: SetlistTrackParticipantRepository,
) {
    // ===== Performance 스코프 (PRD-2) =====

    fun getPerformanceOrThrow(performanceId: UUID): Performance =
        performanceRepository.findByIdOrNull(performanceId)
            ?: throw BusinessException(ErrorCode.PERFORMANCE_NOT_FOUND)

    fun isPerformanceManager(
        performanceId: UUID,
        memberId: Long,
    ): Boolean {
        val performance = getPerformanceOrThrow(performanceId)
        return performanceManagerRepository.existsByPerformanceAndMember(performance, memberId)
    }

    /**
     * 공연 참여자 여부. 공연 매니저이거나, 공연의 셋리스트에 트랙 참여자로 등록되어 있으면 참여자로 본다.
     */
    fun isPerformanceParticipant(
        performanceId: UUID,
        memberId: Long,
    ): Boolean {
        val performance = getPerformanceOrThrow(performanceId)
        if (performanceManagerRepository.existsByPerformanceAndMember(performance, memberId)) return true

        val setlistIds = performance.setlists.map { it.setlistId }
        if (setlistIds.isEmpty()) return false
        return setlistTrackParticipantRepository.existsByTrackSetlistIdInAndMemberId(setlistIds, memberId)
    }

    fun validatePerformanceManager(
        performanceId: UUID,
        memberId: Long,
    ) {
        if (!isPerformanceManager(performanceId, memberId)) {
            throw BusinessException(ErrorCode.NOT_A_PERFORMANCE_MANAGER)
        }
    }

    fun validatePerformanceParticipant(
        performanceId: UUID,
        memberId: Long,
    ) {
        if (!isPerformanceParticipant(performanceId, memberId)) {
            throw BusinessException(ErrorCode.SCHEDULE_PERFORMANCE_FORBIDDEN)
        }
    }

    // ===== 구(舊) Meeting 스코프 (MemberSchedule 폐기 전까지 유지) =====

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
