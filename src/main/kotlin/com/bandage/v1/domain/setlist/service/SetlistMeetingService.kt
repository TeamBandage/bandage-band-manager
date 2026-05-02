package com.bandage.v1.domain.setlist.service

import com.bandage.v1.domain.performance.repository.PerformanceRepository
import com.bandage.v1.domain.setlist.dto.req.SetlistChatMessageCreateRequest
import com.bandage.v1.domain.setlist.dto.req.SetlistConfirmationUpdateRequest
import com.bandage.v1.domain.setlist.dto.req.SetlistItemCreateRequest
import com.bandage.v1.domain.setlist.dto.req.SetlistItemPagingQuery
import com.bandage.v1.domain.setlist.dto.req.SetlistItemUpdateRequest
import com.bandage.v1.domain.setlist.dto.req.SetlistMeetingCreateRequest
import com.bandage.v1.domain.setlist.dto.req.SetlistMeetingPagingQuery
import com.bandage.v1.domain.setlist.dto.req.SetlistMeetingUpdateRequest
import com.bandage.v1.domain.setlist.dto.req.SetlistParticipantsUpdateRequest
import com.bandage.v1.domain.setlist.dto.res.SetlistChatMessageResponse
import com.bandage.v1.domain.setlist.dto.res.SetlistItemResponse
import com.bandage.v1.domain.setlist.dto.res.SetlistLockResponse
import com.bandage.v1.domain.setlist.dto.res.SetlistLockSongMapping
import com.bandage.v1.domain.setlist.dto.res.SetlistMeetingDetailResponse
import com.bandage.v1.domain.setlist.dto.res.SetlistMeetingResponse
import com.bandage.v1.domain.setlist.model.PracticeWindow
import com.bandage.v1.domain.setlist.model.SetlistItem
import com.bandage.v1.domain.setlist.model.SetlistItemApplicant
import com.bandage.v1.domain.setlist.model.SetlistItemChatMessage
import com.bandage.v1.domain.setlist.model.SetlistItemConfirmation
import com.bandage.v1.domain.setlist.model.SetlistMeeting
import com.bandage.v1.domain.setlist.model.SetlistMeetingMember
import com.bandage.v1.domain.setlist.model.enums.MeetingPurpose
import com.bandage.v1.domain.setlist.repository.SetlistItemApplicantRepository
import com.bandage.v1.domain.setlist.repository.SetlistItemChatMessageRepository
import com.bandage.v1.domain.setlist.repository.SetlistItemConfirmationRepository
import com.bandage.v1.domain.setlist.repository.SetlistItemRepository
import com.bandage.v1.domain.setlist.repository.SetlistMeetingMemberRepository
import com.bandage.v1.domain.setlist.repository.SetlistMeetingRepository
import com.bandage.v1.global.common.response.CursorResponse
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional(readOnly = true)
class SetlistMeetingService(
    private val meetingRepository: SetlistMeetingRepository,
    private val meetingMemberRepository: SetlistMeetingMemberRepository,
    private val itemRepository: SetlistItemRepository,
    private val applicantRepository: SetlistItemApplicantRepository,
    private val confirmationRepository: SetlistItemConfirmationRepository,
    private val chatMessageRepository: SetlistItemChatMessageRepository,
    private val performanceRepository: PerformanceRepository,
) {
    @Transactional
    fun createMeeting(
        memberId: Long,
        request: SetlistMeetingCreateRequest,
    ): SetlistMeetingResponse {
        val practiceWindow = resolvePracticeWindow(request)
        val participantIds = (request.participantUserIds + request.managerId + memberId).toSet()
        if (request.managerId !in participantIds) {
            throw BusinessException(ErrorCode.SETLIST_MANAGER_NOT_PARTICIPANT)
        }

        val meeting =
            meetingRepository.save(
                SetlistMeeting.create(
                    bandId = request.bandId,
                    title = request.title,
                    purpose = request.purpose,
                    performanceId = request.performanceId,
                    managerId = request.managerId,
                    practiceWindow = practiceWindow,
                ),
            )
        participantIds.forEach { uid ->
            meetingMemberRepository.save(SetlistMeetingMember.create(meeting = meeting, memberId = uid))
        }
        return SetlistMeetingResponse.of(meeting)
    }

    fun getMeeting(
        meetingId: UUID,
        memberId: Long,
    ): SetlistMeetingDetailResponse {
        val meeting = getMeetingOrThrow(meetingId)
        val members = meetingMemberRepository.findAllByMeeting(meeting)
        validateAccess(meeting, members, memberId)
        return SetlistMeetingDetailResponse.of(meeting, members.map { it.memberId })
    }

    fun getMyMeetings(
        memberId: Long,
        query: SetlistMeetingPagingQuery,
    ): CursorResponse<SetlistMeetingResponse, UUID> {
        val result = meetingRepository.findAllByMemberAndPaging(memberId, query.lastId, query.pageSize)
        return CursorResponse(
            content = result.content.map { SetlistMeetingResponse.of(it) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    @Transactional
    fun updateMeeting(
        meetingId: UUID,
        memberId: Long,
        request: SetlistMeetingUpdateRequest,
    ): SetlistMeetingResponse {
        val meeting = getMeetingOrThrow(meetingId)
        validateManager(meeting, memberId)
        request.title?.let { meeting.updateTitle(it) }
        request.managerId?.let { newManagerId ->
            val members = meetingMemberRepository.findAllByMeeting(meeting)
            if (members.none { it.memberId == newManagerId }) {
                throw BusinessException(ErrorCode.SETLIST_MANAGER_NOT_PARTICIPANT)
            }
            meeting.changeManager(newManagerId)
        }
        return SetlistMeetingResponse.of(meeting)
    }

    @Transactional
    fun deleteMeeting(
        meetingId: UUID,
        memberId: Long,
    ) {
        val meeting = getMeetingOrThrow(meetingId)
        validateManager(meeting, memberId)
        meeting.markAsDeleted(memberId)
    }

    @Transactional
    fun updateParticipants(
        meetingId: UUID,
        memberId: Long,
        request: SetlistParticipantsUpdateRequest,
    ): SetlistMeetingDetailResponse {
        val meeting = getMeetingOrThrow(meetingId)
        validateManager(meeting, memberId)

        val addIds = request.add.toSet()
        val removeIds = request.remove.toSet()
        if (meeting.managerId in removeIds) {
            throw BusinessException(ErrorCode.SETLIST_CANNOT_REMOVE_MANAGER)
        }
        val intersection = addIds intersect removeIds
        if (intersection.isNotEmpty()) {
            throw BusinessException(ErrorCode.SETLIST_MANAGER_NOT_PARTICIPANT)
        }

        // remove cascade: applicants/confirmations
        if (removeIds.isNotEmpty()) {
            val items = itemRepository.findAllByMeeting(meeting)
            removeIds.forEach { uid ->
                meetingMemberRepository.findByMeetingAndMemberId(meeting, uid)?.let { meetingMemberRepository.delete(it) }
                if (items.isNotEmpty()) {
                    applicantRepository.deleteAllByItemInAndMemberId(items, uid)
                    confirmationRepository.deleteAllByItemInAndMemberId(items, uid)
                }
            }
        }
        // add (idempotent)
        addIds.forEach { uid ->
            if (!meetingMemberRepository.existsByMeetingAndMemberId(meeting, uid)) {
                meetingMemberRepository.save(SetlistMeetingMember.create(meeting = meeting, memberId = uid))
            }
        }

        val members = meetingMemberRepository.findAllByMeeting(meeting)
        return SetlistMeetingDetailResponse.of(meeting, members.map { it.memberId })
    }

    // -------- items --------
    @Transactional
    fun createItem(
        meetingId: UUID,
        memberId: Long,
        request: SetlistItemCreateRequest,
    ): SetlistItemResponse {
        val meeting = getMeetingOrThrow(meetingId)
        validateAccess(meeting, memberId)
        if (meeting.isLocked) throw BusinessException(ErrorCode.SETLIST_MEETING_LOCKED)

        val item =
            itemRepository.save(
                SetlistItem.create(
                    meeting = meeting,
                    title = request.title,
                    artist = request.artist,
                    album = request.album,
                    duration = request.duration,
                    proposerId = memberId,
                    note = request.note,
                    sessions = request.sessions.map { it.toEntity() },
                ),
            )
        return SetlistItemResponse.of(item, emptyList(), emptyList())
    }

    fun getItems(
        meetingId: UUID,
        memberId: Long,
        query: SetlistItemPagingQuery,
    ): CursorResponse<SetlistItemResponse, UUID> {
        val meeting = getMeetingOrThrow(meetingId)
        validateAccess(meeting, memberId)
        val result = itemRepository.findAllByMeetingAndPaging(meetingId, query.lastId, query.pageSize)
        if (result.content.isEmpty()) {
            return CursorResponse(content = emptyList(), nextCursor = null, hasNext = false)
        }
        val applicants = applicantRepository.findAllByItemIn(result.content).groupBy { it.item.id }
        val confirmations = confirmationRepository.findAllByItemIn(result.content).groupBy { it.item.id }
        val content =
            result.content.map { item ->
                SetlistItemResponse.of(
                    item = item,
                    applicants = applicants[item.id] ?: emptyList(),
                    confirmations = confirmations[item.id] ?: emptyList(),
                )
            }
        return CursorResponse(content = content, nextCursor = result.nextCursor, hasNext = result.hasNext)
    }

    fun getItem(
        meetingId: UUID,
        itemId: UUID,
        memberId: Long,
    ): SetlistItemResponse {
        val meeting = getMeetingOrThrow(meetingId)
        validateAccess(meeting, memberId)
        val item = getItemOrThrow(meeting, itemId)
        return SetlistItemResponse.of(
            item = item,
            applicants = applicantRepository.findAllByItem(item),
            confirmations = confirmationRepository.findAllByItem(item),
        )
    }

    @Transactional
    fun updateItem(
        meetingId: UUID,
        itemId: UUID,
        memberId: Long,
        request: SetlistItemUpdateRequest,
    ): SetlistItemResponse {
        val meeting = getMeetingOrThrow(meetingId)
        validateAccess(meeting, memberId)
        if (meeting.isLocked) throw BusinessException(ErrorCode.SETLIST_MEETING_LOCKED)
        val item = getItemOrThrow(meeting, itemId)
        if (item.proposerId != memberId && meeting.managerId != memberId) {
            throw BusinessException(ErrorCode.SETLIST_ITEM_FORBIDDEN)
        }
        item.updateMeta(request.title, request.artist, request.album, request.duration, request.note)
        request.sessions?.let { sessions ->
            val newDefs = sessions.map { it.toEntity() }
            val newSessionIds = newDefs.map { it.sessionId }.toSet()
            // cascade — 제거된 세션의 applicant/confirmation 정리
            val applicants = applicantRepository.findAllByItem(item)
            val confirmations = confirmationRepository.findAllByItem(item)
            applicants.filter { it.sessionId !in newSessionIds }.forEach { applicantRepository.delete(it) }
            confirmations.filter { it.sessionId !in newSessionIds }.forEach { confirmationRepository.delete(it) }
            item.replaceSessions(newDefs)
        }
        return SetlistItemResponse.of(
            item = item,
            applicants = applicantRepository.findAllByItem(item),
            confirmations = confirmationRepository.findAllByItem(item),
        )
    }

    @Transactional
    fun deleteItem(
        meetingId: UUID,
        itemId: UUID,
        memberId: Long,
    ) {
        val meeting = getMeetingOrThrow(meetingId)
        validateAccess(meeting, memberId)
        if (meeting.isLocked) throw BusinessException(ErrorCode.SETLIST_MEETING_LOCKED)
        val item = getItemOrThrow(meeting, itemId)
        if (item.proposerId != memberId && meeting.managerId != memberId) {
            throw BusinessException(ErrorCode.SETLIST_ITEM_FORBIDDEN)
        }
        item.markAsDeleted(memberId)
    }

    // -------- applicants --------
    @Transactional
    fun applyForSession(
        meetingId: UUID,
        itemId: UUID,
        sessionId: String,
        memberId: Long,
    ) {
        val meeting = getMeetingOrThrow(meetingId)
        validateAccess(meeting, memberId)
        val item = getItemOrThrow(meeting, itemId)
        validateSessionExists(item, sessionId)
        if (applicantRepository.existsByItemAndSessionIdAndMemberId(item, sessionId, memberId)) return
        applicantRepository.save(SetlistItemApplicant.create(item = item, sessionId = sessionId, memberId = memberId))
    }

    @Transactional
    fun withdrawSessionApplication(
        meetingId: UUID,
        itemId: UUID,
        sessionId: String,
        targetMemberId: Long,
        memberId: Long,
    ) {
        if (targetMemberId != memberId) {
            throw BusinessException(ErrorCode.SETLIST_ITEM_FORBIDDEN)
        }
        val meeting = getMeetingOrThrow(meetingId)
        validateAccess(meeting, memberId)
        val item = getItemOrThrow(meeting, itemId)
        applicantRepository.findByItemAndSessionIdAndMemberId(item, sessionId, memberId)?.let {
            applicantRepository.delete(it)
        }
        // cascade — 확정도 함께 제거
        confirmationRepository.findByItemAndSessionIdAndMemberId(item, sessionId, memberId)?.let {
            confirmationRepository.delete(it)
        }
    }

    // -------- confirmations --------
    @Transactional
    fun updateConfirmations(
        meetingId: UUID,
        itemId: UUID,
        sessionId: String,
        memberId: Long,
        request: SetlistConfirmationUpdateRequest,
    ): SetlistItemResponse {
        val meeting = getMeetingOrThrow(meetingId)
        validateManager(meeting, memberId)
        val item = getItemOrThrow(meeting, itemId)
        val sessionDef =
            item.sessions.firstOrNull { it.sessionId == sessionId }
                ?: throw BusinessException(ErrorCode.SETLIST_ITEM_SESSION_NOT_FOUND)

        request.unconfirm.forEach { uid ->
            confirmationRepository.findByItemAndSessionIdAndMemberId(item, sessionId, uid)?.let {
                confirmationRepository.delete(it)
            }
        }
        val currentCount = confirmationRepository.countByItemAndSessionId(item, sessionId).toInt()
        val toAdd =
            request.confirm.filter { uid ->
                confirmationRepository.findByItemAndSessionIdAndMemberId(item, sessionId, uid) == null
            }
        if (currentCount + toAdd.size > sessionDef.need) {
            throw BusinessException(ErrorCode.SETLIST_ITEM_SESSION_FULL)
        }
        toAdd.forEach { uid ->
            confirmationRepository.save(
                SetlistItemConfirmation.create(item = item, sessionId = sessionId, memberId = uid, confirmedBy = memberId),
            )
        }
        return SetlistItemResponse.of(
            item = item,
            applicants = applicantRepository.findAllByItem(item),
            confirmations = confirmationRepository.findAllByItem(item),
        )
    }

    // -------- chat --------
    fun getChatMessages(
        meetingId: UUID,
        itemId: UUID,
        memberId: Long,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<SetlistChatMessageResponse, UUID> {
        val meeting = getMeetingOrThrow(meetingId)
        validateAccess(meeting, memberId)
        val item = getItemOrThrow(meeting, itemId)
        val result = chatMessageRepository.findAllByItemAndPaging(item.id, lastId, pageSize)
        return CursorResponse(
            content = result.content.map { SetlistChatMessageResponse.of(it) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    @Transactional
    fun createChatMessage(
        meetingId: UUID,
        itemId: UUID,
        memberId: Long,
        request: SetlistChatMessageCreateRequest,
    ): SetlistChatMessageResponse {
        val meeting = getMeetingOrThrow(meetingId)
        validateAccess(meeting, memberId)
        val item = getItemOrThrow(meeting, itemId)
        val msg =
            chatMessageRepository.save(
                SetlistItemChatMessage.create(item = item, memberId = memberId, message = request.message),
            )
        return SetlistChatMessageResponse.of(msg)
    }

    // -------- lock / unlock --------
    @Transactional
    fun lockMeeting(
        meetingId: UUID,
        memberId: Long,
    ): SetlistLockResponse {
        val meeting = getMeetingOrThrow(meetingId)
        validateManager(meeting, memberId)
        if (meeting.isLocked) throw BusinessException(ErrorCode.SETLIST_MEETING_LOCKED)
        meeting.lock()
        // TODO: PracticeSong 벌크 생성 + Performance.setlist 자동 등록 (cross-domain)
        val items = itemRepository.findAllByMeeting(meeting)
        return SetlistLockResponse(
            lockedAt = meeting.lockedAt!!,
            songs = items.map { SetlistLockSongMapping(setlistItemId = it.id, practiceSongId = it.practiceSongId) },
        )
    }

    @Transactional
    fun unlockMeeting(
        meetingId: UUID,
        memberId: Long,
    ): SetlistMeetingResponse {
        val meeting = getMeetingOrThrow(meetingId)
        validateManager(meeting, memberId)
        if (!meeting.isLocked) throw BusinessException(ErrorCode.SETLIST_MEETING_NOT_LOCKED)
        meeting.unlock()
        return SetlistMeetingResponse.of(meeting)
    }

    // -------- internal --------
    private fun getMeetingOrThrow(meetingId: UUID): SetlistMeeting =
        meetingRepository.findByIdOrNull(meetingId)
            ?: throw BusinessException(ErrorCode.SETLIST_MEETING_NOT_FOUND)

    private fun getItemOrThrow(
        meeting: SetlistMeeting,
        itemId: UUID,
    ): SetlistItem {
        val item =
            itemRepository.findByIdOrNull(itemId)
                ?: throw BusinessException(ErrorCode.SETLIST_ITEM_NOT_FOUND)
        if (item.meeting.id != meeting.id) throw BusinessException(ErrorCode.SETLIST_ITEM_NOT_FOUND)
        return item
    }

    private fun validateAccess(
        meeting: SetlistMeeting,
        memberId: Long,
    ) {
        if (meeting.managerId == memberId) return
        if (!meetingMemberRepository.existsByMeetingAndMemberId(meeting, memberId)) {
            throw BusinessException(ErrorCode.SETLIST_MEETING_FORBIDDEN)
        }
    }

    private fun validateAccess(
        meeting: SetlistMeeting,
        members: List<SetlistMeetingMember>,
        memberId: Long,
    ) {
        if (meeting.managerId == memberId) return
        if (members.none { it.memberId == memberId }) {
            throw BusinessException(ErrorCode.SETLIST_MEETING_FORBIDDEN)
        }
    }

    private fun validateManager(
        meeting: SetlistMeeting,
        memberId: Long,
    ) {
        if (meeting.managerId != memberId) throw BusinessException(ErrorCode.SETLIST_MEETING_NOT_MANAGER)
    }

    private fun validateSessionExists(
        item: SetlistItem,
        sessionId: String,
    ) {
        if (item.sessions.none { it.sessionId == sessionId }) {
            throw BusinessException(ErrorCode.SETLIST_ITEM_SESSION_NOT_FOUND)
        }
    }

    private fun resolvePracticeWindow(request: SetlistMeetingCreateRequest): PracticeWindow {
        if (request.purpose == MeetingPurpose.PERFORMANCE) {
            if (request.performanceId == null) throw BusinessException(ErrorCode.SETLIST_PERFORMANCE_REQUIRED)
            if (meetingRepository.existsByPerformanceIdAndLockedAtIsNull(request.performanceId)) {
                throw BusinessException(ErrorCode.SETLIST_PERFORMANCE_HAS_ACTIVE_MEETING)
            }
            val performance =
                performanceRepository.findByIdOrNull(request.performanceId)
                    ?: throw BusinessException(ErrorCode.PERFORMANCE_NOT_FOUND)
            val from = LocalDate.now()
            val to =
                performance.schedule.startAt
                    .toLocalDate()
                    .minusDays(1)
            if (from.isAfter(to)) throw BusinessException(ErrorCode.SETLIST_PRACTICE_WINDOW_INVALID)
            return PracticeWindow(from = from, to = to)
        }
        val window =
            request.practiceWindow
                ?: throw BusinessException(ErrorCode.SETLIST_PRACTICE_WINDOW_REQUIRED)
        if (window.from.isAfter(window.to)) {
            throw BusinessException(ErrorCode.SETLIST_PRACTICE_WINDOW_INVALID)
        }
        return window.toEntity()
    }
}
