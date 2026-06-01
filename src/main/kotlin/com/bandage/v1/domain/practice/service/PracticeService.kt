package com.bandage.v1.domain.practice.service

import com.bandage.v1.domain.band.repository.BandMemberRepository
import com.bandage.v1.domain.practice.dto.req.PracticeCreateRequest
import com.bandage.v1.domain.practice.dto.req.PracticeMemberAddRequest
import com.bandage.v1.domain.practice.dto.req.PracticePagingQuery
import com.bandage.v1.domain.practice.dto.req.PracticeSearchQuery
import com.bandage.v1.domain.practice.dto.req.PracticeSessionsUpdateRequest
import com.bandage.v1.domain.practice.dto.req.PracticeTimeInfoUpdateRequest
import com.bandage.v1.domain.practice.dto.req.PracticeVenueUpdateRequest
import com.bandage.v1.domain.practice.dto.res.PracticeDetailResponse
import com.bandage.v1.domain.practice.dto.res.PracticeListResponse
import com.bandage.v1.domain.practice.dto.res.PracticeParticipantResponse
import com.bandage.v1.domain.practice.dto.res.PracticeResponse
import com.bandage.v1.domain.practice.model.Practice
import com.bandage.v1.domain.practice.model.PracticeParticipant
import com.bandage.v1.domain.practice.repository.PracticeParticipantRepository
import com.bandage.v1.domain.practice.repository.PracticeRepository
import com.bandage.v1.global.common.response.CursorResponse
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class PracticeService(
    private val practiceRepository: PracticeRepository,
    private val practiceParticipantRepository: PracticeParticipantRepository,
    private val bandMemberRepository: BandMemberRepository,
) {
    @Transactional
    fun createPractice(request: PracticeCreateRequest): PracticeResponse {
        val trackInfo = request.track.toEntity()
        val practice =
            practiceRepository.save(
                Practice.create(
                    title = request.title ?: trackInfo.title,
                    trackInfo = trackInfo,
                    startAt = request.startAt,
                    durationMinutes = request.durationMinutes,
                    venue = request.venue,
                    note = request.note,
                    sessions = request.sessions.map { it.toEntity() },
                ),
            )
        return PracticeResponse.of(practice)
    }

    fun getPracticeDetail(practiceId: UUID): PracticeDetailResponse = PracticeDetailResponse.of(getPractice(practiceId))

    fun getPracticesByCursor(
        bandId: UUID?,
        query: PracticePagingQuery,
    ): CursorResponse<PracticeListResponse, UUID> {
        val result =
            if (bandId != null) {
                val memberIds = bandMemberRepository.findAllMemberIdsByBand(bandId)
                if (memberIds.isEmpty()) {
                    return CursorResponse(content = emptyList(), nextCursor = null, hasNext = false)
                }
                practiceRepository.findAllByMembersAndPaging(memberIds, query.lastId, query.pageSize)
            } else {
                practiceRepository.findAllByPaging(query.lastId, query.pageSize)
            }
        return CursorResponse(
            content = result.content.map { PracticeListResponse.of(it) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    fun getMyPracticesByCursor(
        memberId: Long,
        query: PracticePagingQuery,
    ): CursorResponse<PracticeListResponse, UUID> {
        val result = practiceRepository.findAllByMemberAndPaging(memberId, query.lastId, query.pageSize)
        return CursorResponse(
            content = result.content.map { PracticeListResponse.of(it) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    fun searchMyPracticesByCursor(
        memberId: Long,
        query: PracticeSearchQuery,
    ): CursorResponse<PracticeListResponse, UUID> {
        val result =
            practiceRepository.searchByMemberAndKeywordAndPaging(
                memberId = memberId,
                keyword = query.keyword,
                lastId = query.lastId,
                pageSize = query.pageSize,
            )
        return CursorResponse(
            content = result.content.map { PracticeListResponse.of(it) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    @Transactional
    fun updateSessions(
        practiceId: UUID,
        request: PracticeSessionsUpdateRequest,
    ): PracticeDetailResponse {
        val practice = getPractice(practiceId)
        val newDefs = request.sessions.map { it.toEntity() }
        val newSessionIds = newDefs.map { it.sessionId }.toSet()
        practiceParticipantRepository
            .findAllByPractice(practice)
            .filter { it.sessionId !in newSessionIds }
            .forEach { practiceParticipantRepository.delete(it) }
        practice.replaceSessions(newDefs)
        return PracticeDetailResponse.of(practice)
    }

    @Transactional
    fun addParticipant(
        practiceId: UUID,
        request: PracticeMemberAddRequest,
    ): PracticeParticipantResponse {
        val practice = getPractice(practiceId)
        validateSessionExists(practice, request.sessionId)
        validateParticipantNotExists(practice, request.sessionId, request.memberId)
        val participant =
            practiceParticipantRepository.save(
                PracticeParticipant.create(
                    practice = practice,
                    sessionId = request.sessionId,
                    member = request.memberId,
                ),
            )
        return PracticeParticipantResponse.of(participant)
    }

    @Transactional
    fun deleteParticipant(
        practiceId: UUID,
        participantId: UUID,
        memberId: Long,
    ) {
        val practice = getPractice(practiceId)
        val participant =
            practiceParticipantRepository.findByIdAndPractice(participantId, practice)
                ?: throw BusinessException(ErrorCode.PRACTICE_PARTICIPANT_NOT_FOUND)
        participant.markAsDeleted(memberId)
    }

    @Transactional
    fun updateTimeInfo(
        practiceId: UUID,
        request: PracticeTimeInfoUpdateRequest,
    ) {
        val practice = getPractice(practiceId)
        practice.updateTimeInfo(request.startAt, request.durationMinutes)
    }

    @Transactional
    fun updateVenue(
        practiceId: UUID,
        request: PracticeVenueUpdateRequest,
    ) {
        val practice = getPractice(practiceId)
        practice.updateVenue(request.venue)
    }

    @Transactional
    fun deletePractice(
        practiceId: UUID,
        memberId: Long,
    ) {
        val practice = getPractice(practiceId)
        practice.markAsDeleted(memberId)
    }

    // --- 내부 유틸리티 메서드 ---
    private fun getPractice(practiceId: UUID): Practice =
        practiceRepository.findByIdOrNull(practiceId)
            ?: throw BusinessException(ErrorCode.PRACTICE_NOT_FOUND)

    private fun validateSessionExists(
        practice: Practice,
        sessionId: String,
    ) {
        if (practice.sessions.none { it.sessionId == sessionId }) {
            throw BusinessException(ErrorCode.PRACTICE_SESSION_NOT_FOUND)
        }
    }

    private fun validateParticipantNotExists(
        practice: Practice,
        sessionId: String,
        memberId: Long,
    ) {
        if (practiceParticipantRepository.existsByPracticeAndSessionIdAndMember(practice, sessionId, memberId)) {
            throw BusinessException(ErrorCode.PRACTICE_PARTICIPANT_ALREADY_EXISTS)
        }
    }
}
