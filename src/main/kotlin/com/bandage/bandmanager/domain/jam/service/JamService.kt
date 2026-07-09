package com.bandage.bandmanager.domain.jam.service

import com.bandage.bandmanager.domain.band.repository.BandMemberRepository
import com.bandage.bandmanager.domain.jam.dto.req.JamCreateRequest
import com.bandage.bandmanager.domain.jam.dto.req.JamMemberAddRequest
import com.bandage.bandmanager.domain.jam.dto.req.JamPagingQuery
import com.bandage.bandmanager.domain.jam.dto.req.JamParticipantSessionUpdateRequest
import com.bandage.bandmanager.domain.jam.dto.req.JamSearchQuery
import com.bandage.bandmanager.domain.jam.dto.req.JamSessionAddRequest
import com.bandage.bandmanager.domain.jam.dto.req.JamSessionUpdateRequest
import com.bandage.bandmanager.domain.jam.dto.req.JamSessionsUpdateRequest
import com.bandage.bandmanager.domain.jam.dto.req.JamTimeInfoUpdateRequest
import com.bandage.bandmanager.domain.jam.dto.req.JamVenueUpdateRequest
import com.bandage.bandmanager.domain.jam.dto.res.JamDetailResponse
import com.bandage.bandmanager.domain.jam.dto.res.JamListResponse
import com.bandage.bandmanager.domain.jam.dto.res.JamParticipantResponse
import com.bandage.bandmanager.domain.jam.dto.res.JamResponse
import com.bandage.bandmanager.domain.jam.model.Jam
import com.bandage.bandmanager.domain.jam.model.JamParticipant
import com.bandage.bandmanager.domain.jam.repository.JamParticipantRepository
import com.bandage.bandmanager.domain.jam.repository.JamRepository
import com.bandage.bandmanager.domain.member.dto.res.MemberSummary
import com.bandage.bandmanager.domain.member.service.MemberService
import com.bandage.bandmanager.global.common.domain.SessionDef
import com.bandage.bandmanager.global.common.response.CursorResponse
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class JamService(
    private val jamRepository: JamRepository,
    private val jamParticipantRepository: JamParticipantRepository,
    private val bandMemberRepository: BandMemberRepository,
    private val jamReservationSyncService: JamReservationSyncService,
    private val memberService: MemberService,
) {
    @Transactional
    fun createJam(
        request: JamCreateRequest,
        memberId: Long,
    ): JamResponse {
        val trackInfo = request.track.toEntity()
        val jam =
            jamRepository.save(
                Jam.create(
                    title = request.title ?: trackInfo.title,
                    trackInfo = trackInfo,
                    startAt = request.startAt,
                    durationMinutes = request.durationMinutes,
                    venue = request.venue,
                    note = request.note,
                    sessions = request.sessions.map { it.toEntity() },
                ),
            )
        // 생성자를 세션 미배정(소속) 참여자로 자동 등록 → "내 합주 목록" 노출. 세션은 추후 세션 변경 API로 지정.
        jamParticipantRepository.save(
            JamParticipant.create(jam = jam, sessionId = null, member = memberId),
        )
        jamReservationSyncService.sync(jam)
        return JamResponse.of(jam)
    }

    fun getJamDetail(jamId: UUID): JamDetailResponse = toDetailResponse(getJam(jamId))

    fun getJamsByCursor(
        bandId: UUID?,
        query: JamPagingQuery,
    ): CursorResponse<JamListResponse, UUID> {
        val result =
            if (bandId != null) {
                val memberIds = bandMemberRepository.findAllMemberIdsByBand(bandId)
                if (memberIds.isEmpty()) {
                    return CursorResponse(content = emptyList(), nextCursor = null, hasNext = false)
                }
                jamRepository.findAllByMembersAndPaging(memberIds, query.lastId, query.pageSize)
            } else {
                jamRepository.findAllByPaging(query.lastId, query.pageSize)
            }
        return CursorResponse(
            content = result.content.map { JamListResponse.of(it) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    fun getMyJamsByCursor(
        memberId: Long,
        query: JamPagingQuery,
    ): CursorResponse<JamListResponse, UUID> {
        val result = jamRepository.findAllByMemberAndPaging(memberId, query.lastId, query.pageSize)
        return CursorResponse(
            content = result.content.map { JamListResponse.of(it) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    fun searchMyJamsByCursor(
        memberId: Long,
        query: JamSearchQuery,
    ): CursorResponse<JamListResponse, UUID> {
        val result =
            jamRepository.searchByMemberAndKeywordAndPaging(
                memberId = memberId,
                keyword = query.keyword,
                lastId = query.lastId,
                pageSize = query.pageSize,
            )
        return CursorResponse(
            content = result.content.map { JamListResponse.of(it) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    @Transactional
    fun updateSessions(
        jamId: UUID,
        request: JamSessionsUpdateRequest,
        memberId: Long,
    ): JamDetailResponse {
        val jam = getJam(jamId)
        validateParticipant(jam, memberId)
        val newDefs = request.sessions.map { it.toEntity() }
        val newSessionIds = newDefs.map { it.sessionId }.toSet()
        deleteParticipantsNotIn(jam, newSessionIds)
        jam.replaceSessions(newDefs)
        jamReservationSyncService.sync(jam)
        return toDetailResponse(jam)
    }

    @Transactional
    fun addSession(
        jamId: UUID,
        request: JamSessionAddRequest,
        memberId: Long,
    ): JamDetailResponse {
        val jam = getJam(jamId)
        validateParticipant(jam, memberId)
        validateSessionNotExists(jam, request.sessionId)
        jam.addSession(
            SessionDef(
                sessionId = request.sessionId,
                label = request.label,
                short = request.short,
                custom = request.custom,
            ),
        )
        return toDetailResponse(jam)
    }

    @Transactional
    fun updateSession(
        jamId: UUID,
        sessionId: String,
        request: JamSessionUpdateRequest,
        memberId: Long,
    ): JamDetailResponse {
        val jam = getJam(jamId)
        validateParticipant(jam, memberId)
        validateSessionExists(jam, sessionId)
        jam.updateSession(sessionId, request.label, request.short)
        return toDetailResponse(jam)
    }

    @Transactional
    fun removeSession(
        jamId: UUID,
        sessionId: String,
        memberId: Long,
    ): JamDetailResponse {
        val jam = getJam(jamId)
        validateParticipant(jam, memberId)
        validateSessionExists(jam, sessionId)
        deleteParticipantsNotIn(jam, jam.sessions.map { it.sessionId }.toSet() - sessionId)
        jam.removeSession(sessionId)
        jamReservationSyncService.sync(jam)
        return toDetailResponse(jam)
    }

    @Transactional
    fun addParticipant(
        jamId: UUID,
        request: JamMemberAddRequest,
        memberId: Long,
    ): JamParticipantResponse {
        val jam = getJam(jamId)
        validateParticipant(jam, memberId)
        validateSessionExists(jam, request.sessionId)
        validateParticipantNotExists(jam, request.sessionId, request.memberId)
        validateSessionNotFull(jam, request.sessionId)
        val participant =
            jamParticipantRepository.save(
                JamParticipant.create(
                    jam = jam,
                    sessionId = request.sessionId,
                    member = request.memberId,
                ),
            )
        jamReservationSyncService.sync(jam)
        return JamParticipantResponse.of(participant, summaryOf(participant.member))
    }

    @Transactional
    fun updateParticipantSession(
        jamId: UUID,
        participantId: UUID,
        request: JamParticipantSessionUpdateRequest,
        memberId: Long,
    ): JamParticipantResponse {
        val jam = getJam(jamId)
        validateParticipant(jam, memberId)
        val participant =
            jamParticipantRepository.findByIdAndJam(participantId, jam)
                ?: throw BusinessException(ErrorCode.JAM_PARTICIPANT_NOT_FOUND)
        if (participant.sessionId == request.sessionId) {
            return JamParticipantResponse.of(participant, summaryOf(participant.member))
        }
        validateSessionExists(jam, request.sessionId)
        validateParticipantNotExists(jam, request.sessionId, participant.member)
        validateSessionNotFull(jam, request.sessionId)
        participant.changeSession(request.sessionId)
        return JamParticipantResponse.of(participant, summaryOf(participant.member))
    }

    @Transactional
    fun deleteParticipant(
        jamId: UUID,
        participantId: UUID,
        memberId: Long,
    ) {
        val jam = getJam(jamId)
        validateParticipant(jam, memberId)
        val participant =
            jamParticipantRepository.findByIdAndJam(participantId, jam)
                ?: throw BusinessException(ErrorCode.JAM_PARTICIPANT_NOT_FOUND)
        participant.markAsDeleted(memberId)
        jamReservationSyncService.sync(jam)
    }

    @Transactional
    fun updateTimeInfo(
        jamId: UUID,
        request: JamTimeInfoUpdateRequest,
        memberId: Long,
    ) {
        val jam = getJam(jamId)
        validateParticipant(jam, memberId)
        jam.updateTimeInfo(request.startAt, request.durationMinutes)
        jamReservationSyncService.sync(jam)
    }

    @Transactional
    fun updateVenue(
        jamId: UUID,
        request: JamVenueUpdateRequest,
        memberId: Long,
    ) {
        val jam = getJam(jamId)
        validateParticipant(jam, memberId)
        jam.updateVenue(request.venue)
    }

    @Transactional
    fun deleteJam(
        jamId: UUID,
        memberId: Long,
    ) {
        val jam = getJam(jamId)
        validateParticipant(jam, memberId)
        jam.markAsDeleted(memberId)
        jamReservationSyncService.sync(jam)
    }

    // --- 내부 유틸리티 메서드 ---
    private fun getJam(jamId: UUID): Jam =
        jamRepository.findByIdOrNull(jamId)
            ?: throw BusinessException(ErrorCode.JAM_NOT_FOUND)

    /** 합주 참여자 전원의 회원 정보를 한 번에 조회해 상세 응답을 구성한다(N+1 방지). */
    private fun toDetailResponse(jam: Jam): JamDetailResponse {
        val members = memberService.getMemberSummaries(jam.participants.map { it.member })
        return JamDetailResponse.of(jam, members)
    }

    private fun summaryOf(memberId: Long): MemberSummary? = memberService.getMemberSummaries(listOf(memberId))[memberId]

    private fun validateParticipant(
        jam: Jam,
        memberId: Long,
    ) {
        if (!jamParticipantRepository.existsByJamAndMember(jam, memberId)) {
            throw BusinessException(ErrorCode.JAM_FORBIDDEN_NOT_PARTICIPANT)
        }
    }

    private fun validateSessionExists(
        jam: Jam,
        sessionId: String,
    ) {
        if (jam.sessions.none { it.sessionId == sessionId }) {
            throw BusinessException(ErrorCode.JAM_SESSION_NOT_FOUND)
        }
    }

    private fun validateSessionNotExists(
        jam: Jam,
        sessionId: String,
    ) {
        if (jam.sessions.any { it.sessionId == sessionId }) {
            throw BusinessException(ErrorCode.JAM_SESSION_ALREADY_EXISTS)
        }
    }

    /**
     * 남길 세션(keepSessionIds)에 포함되지 않은 참여자 배정을 정리한다(세션 전체 교체/개별 삭제 공용).
     * 세션 미배정 소속 참여자(sessionId=null, 생성자 등)는 세션 목록과 무관하므로 대상에서 제외한다.
     */
    private fun deleteParticipantsNotIn(
        jam: Jam,
        keepSessionIds: Set<String>,
    ) {
        jamParticipantRepository
            .findAllByJam(jam)
            .filter { it.sessionId != null && it.sessionId !in keepSessionIds }
            .forEach { jamParticipantRepository.delete(it) }
    }

    private fun validateSessionNotFull(
        jam: Jam,
        sessionId: String,
    ) {
        if (jamParticipantRepository.existsByJamAndSessionId(jam, sessionId)) {
            throw BusinessException(ErrorCode.JAM_SESSION_FULL)
        }
    }

    private fun validateParticipantNotExists(
        jam: Jam,
        sessionId: String,
        memberId: Long,
    ) {
        if (jamParticipantRepository.existsByJamAndSessionIdAndMember(jam, sessionId, memberId)) {
            throw BusinessException(ErrorCode.JAM_PARTICIPANT_ALREADY_EXISTS)
        }
    }
}
