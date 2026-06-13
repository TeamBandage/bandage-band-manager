package com.bandage.bandmanager.domain.schedule.service

import com.bandage.bandmanager.domain.schedule.dto.req.MemberScheduleRequest
import com.bandage.bandmanager.domain.schedule.dto.res.MemberScheduleAggregateResponse
import com.bandage.bandmanager.domain.schedule.dto.res.MemberScheduleResponse
import com.bandage.bandmanager.domain.schedule.model.MemberSchedule
import com.bandage.bandmanager.domain.schedule.repository.MemberScheduleRepository
import com.bandage.bandmanager.domain.selection.model.PracticeWindow
import com.bandage.bandmanager.domain.selection.model.TrackSelection
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionMemberRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionRepository
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional(readOnly = true)
class MemberScheduleService(
    private val memberScheduleRepository: MemberScheduleRepository,
    private val trackSelectionRepository: TrackSelectionRepository,
    private val trackSelectionMemberRepository: TrackSelectionMemberRepository,
    private val scheduleAuthService: ScheduleAuthService,
) {
    fun getMySchedule(
        meetingId: UUID,
        memberId: Long,
    ): MemberScheduleResponse {
        scheduleAuthService.validateParticipant(meetingId, memberId)
        val schedule = memberScheduleRepository.findByMeetingIdAndUserId(meetingId, memberId)
        return schedule
            ?.let { MemberScheduleResponse.from(it) }
            ?: MemberScheduleResponse.empty(memberId)
    }

    @Transactional
    fun upsertMySchedule(
        meetingId: UUID,
        memberId: Long,
        request: MemberScheduleRequest,
    ): MemberScheduleResponse {
        scheduleAuthService.validateParticipant(meetingId, memberId)
        val meeting = getMeetingOrThrow(meetingId)
        validateRequestDates(request, meeting.practiceWindow)

        val schedule =
            memberScheduleRepository.findByMeetingIdAndUserId(meetingId, memberId)
                ?: MemberSchedule.create(meetingId = meetingId, userId = memberId, note = request.note)

        schedule.updateAvailability(
            availableDates = request.availableDates,
            unavailableDates = request.unavailableDates,
            blocks = request.blocks,
        )
        if (request.note !== null || schedule.note != null) {
            schedule.updateNote(request.note ?: schedule.note)
        }
        when (request.completed) {
            true -> schedule.complete()
            false -> schedule.reopen()
            null -> Unit
        }
        val saved = memberScheduleRepository.save(schedule)
        return MemberScheduleResponse.from(saved)
    }

    fun getAllSchedules(
        meetingId: UUID,
        memberId: Long,
    ): List<MemberScheduleResponse> {
        scheduleAuthService.validateParticipant(meetingId, memberId)
        return memberScheduleRepository
            .findAllByMeetingId(meetingId)
            .map { MemberScheduleResponse.from(it) }
    }

    fun getAggregatedSchedule(
        meetingId: UUID,
        memberId: Long,
    ): MemberScheduleAggregateResponse {
        scheduleAuthService.validateParticipant(meetingId, memberId)
        val meeting = getMeetingOrThrow(meetingId)
        val participants = trackSelectionMemberRepository.findAllBySelectionId(meetingId)
        val totalParticipants = participants.size
        val schedules = memberScheduleRepository.findAllByMeetingId(meetingId)
        val completedCount = schedules.count { it.completed }

        val window = meeting.practiceWindow
        val dateAvailability = mutableMapOf<LocalDate, MemberScheduleAggregateResponse.AvailabilityCount>()
        var date = window.from
        while (!date.isAfter(window.to)) {
            val available = schedules.count { date in it.availableDates }
            val unavailable = schedules.count { date in it.unavailableDates }
            val responded = available + unavailable
            val pending = (totalParticipants - responded).coerceAtLeast(0)
            dateAvailability[date] =
                MemberScheduleAggregateResponse.AvailabilityCount(
                    available = available,
                    unavailable = unavailable,
                    pending = pending,
                )
            date = date.plusDays(1)
        }

        return MemberScheduleAggregateResponse(
            dateAvailability = dateAvailability,
            totalParticipants = totalParticipants,
            completedCount = completedCount,
        )
    }

    private fun getMeetingOrThrow(meetingId: UUID): TrackSelection =
        trackSelectionRepository.findByIdOrNull(meetingId)
            ?: throw BusinessException(ErrorCode.SETLIST_MEETING_NOT_FOUND)

    private fun validateRequestDates(
        request: MemberScheduleRequest,
        window: PracticeWindow,
    ) {
        val available = request.availableDates ?: emptySet()
        val unavailable = request.unavailableDates ?: emptySet()
        val blockKeys = request.blocks?.keys ?: emptySet()

        if (available.intersect(unavailable).isNotEmpty()) {
            throw BusinessException(ErrorCode.SCHEDULE_DATES_OVERLAP)
        }

        val all = available + unavailable + blockKeys
        if (all.any { it.isBefore(window.from) || it.isAfter(window.to) }) {
            throw BusinessException(ErrorCode.SCHEDULE_DATE_OUT_OF_WINDOW)
        }
    }
}
