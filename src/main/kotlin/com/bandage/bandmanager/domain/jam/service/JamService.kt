package com.bandage.bandmanager.domain.jam.service

import com.bandage.bandmanager.domain.band.repository.BandMemberRepository
import com.bandage.bandmanager.domain.jam.dto.req.JamCreateRequest
import com.bandage.bandmanager.domain.jam.dto.req.JamMemberAddRequest
import com.bandage.bandmanager.domain.jam.dto.req.JamPagingQuery
import com.bandage.bandmanager.domain.jam.dto.req.JamSearchQuery
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
) {
    @Transactional
    fun createJam(request: JamCreateRequest): JamResponse {
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
        jamReservationSyncService.sync(jam)
        return JamResponse.of(jam)
    }

    fun getJamDetail(jamId: UUID): JamDetailResponse = JamDetailResponse.of(getJam(jamId))

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
    ): JamDetailResponse {
        val jam = getJam(jamId)
        val newDefs = request.sessions.map { it.toEntity() }
        val newSessionIds = newDefs.map { it.sessionId }.toSet()
        jamParticipantRepository
            .findAllByJam(jam)
            .filter { it.sessionId !in newSessionIds }
            .forEach { jamParticipantRepository.delete(it) }
        jam.replaceSessions(newDefs)
        jamReservationSyncService.sync(jam)
        return JamDetailResponse.of(jam)
    }

    @Transactional
    fun addParticipant(
        jamId: UUID,
        request: JamMemberAddRequest,
    ): JamParticipantResponse {
        val jam = getJam(jamId)
        validateSessionExists(jam, request.sessionId)
        validateParticipantNotExists(jam, request.sessionId, request.memberId)
        val participant =
            jamParticipantRepository.save(
                JamParticipant.create(
                    jam = jam,
                    sessionId = request.sessionId,
                    member = request.memberId,
                ),
            )
        jamReservationSyncService.sync(jam)
        return JamParticipantResponse.of(participant)
    }

    @Transactional
    fun deleteParticipant(
        jamId: UUID,
        participantId: UUID,
        memberId: Long,
    ) {
        val jam = getJam(jamId)
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
    ) {
        val jam = getJam(jamId)
        jam.updateTimeInfo(request.startAt, request.durationMinutes)
        jamReservationSyncService.sync(jam)
    }

    @Transactional
    fun updateVenue(
        jamId: UUID,
        request: JamVenueUpdateRequest,
    ) {
        val jam = getJam(jamId)
        jam.updateVenue(request.venue)
    }

    @Transactional
    fun deleteJam(
        jamId: UUID,
        memberId: Long,
    ) {
        val jam = getJam(jamId)
        jam.markAsDeleted(memberId)
        jamReservationSyncService.sync(jam)
    }

    // --- 내부 유틸리티 메서드 ---
    private fun getJam(jamId: UUID): Jam =
        jamRepository.findByIdOrNull(jamId)
            ?: throw BusinessException(ErrorCode.JAM_NOT_FOUND)

    private fun validateSessionExists(
        jam: Jam,
        sessionId: String,
    ) {
        if (jam.sessions.none { it.sessionId == sessionId }) {
            throw BusinessException(ErrorCode.JAM_SESSION_NOT_FOUND)
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
