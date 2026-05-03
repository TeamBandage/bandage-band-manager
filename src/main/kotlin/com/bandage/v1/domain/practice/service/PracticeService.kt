package com.bandage.v1.domain.practice.service

import com.bandage.v1.domain.band.repository.BandMemberRepository
import com.bandage.v1.domain.practice.dto.Song
import com.bandage.v1.domain.practice.dto.req.PracticeCreateRequest
import com.bandage.v1.domain.practice.dto.req.PracticeMemberAddRequest
import com.bandage.v1.domain.practice.dto.req.PracticePagingQuery
import com.bandage.v1.domain.practice.dto.req.PracticeSearchQuery
import com.bandage.v1.domain.practice.dto.req.PracticeSessionCreateRequest
import com.bandage.v1.domain.practice.dto.req.PracticeSongRefLinkUpsertRequest
import com.bandage.v1.domain.practice.dto.req.PracticeSongUpdateRequest
import com.bandage.v1.domain.practice.dto.req.PracticeTimeInfoUpdateRequest
import com.bandage.v1.domain.practice.dto.req.PracticeVenueUpdateRequest
import com.bandage.v1.domain.practice.dto.res.PracticeDetailResponse
import com.bandage.v1.domain.practice.dto.res.PracticeListResponse
import com.bandage.v1.domain.practice.dto.res.PracticeParticipantResponse
import com.bandage.v1.domain.practice.dto.res.PracticeResponse
import com.bandage.v1.domain.practice.dto.res.PracticeSessionResponse
import com.bandage.v1.domain.practice.dto.res.PracticeSongResponse
import com.bandage.v1.domain.practice.model.Practice
import com.bandage.v1.domain.practice.model.PracticeParticipant
import com.bandage.v1.domain.practice.model.PracticeSession
import com.bandage.v1.domain.practice.model.PracticeSong
import com.bandage.v1.domain.practice.repository.PracticeParticipantRepository
import com.bandage.v1.domain.practice.repository.PracticeRepository
import com.bandage.v1.domain.practice.repository.PracticeSessionRepository
import com.bandage.v1.domain.practice.repository.PracticeSongRepository
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
    private val practiceSessionRepository: PracticeSessionRepository,
    private val practiceParticipantRepository: PracticeParticipantRepository,
    private val practiceSongRepository: PracticeSongRepository,
    private val bandMemberRepository: BandMemberRepository,
) {
    @Transactional
    fun createPractice(request: PracticeCreateRequest): PracticeResponse {
        val song = getPracticeSong(request.song)
        val practice =
            practiceRepository.save(
                Practice.create(
                    title = request.title ?: song.title,
                    song = song,
                    venue = request.venue,
                    startAt = request.startAt,
                    durationMinutes = request.durationMinutes,
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
    fun createSession(
        practiceId: UUID,
        request: PracticeSessionCreateRequest,
    ): PracticeSessionResponse {
        val practice = getPractice(practiceId)
        val session =
            practiceSessionRepository.save(
                PracticeSession.create(
                    practice = practice,
                    label = request.label,
                    type = request.type,
                    participant = null,
                ),
            )
        return PracticeSessionResponse.of(session)
    }

    @Transactional
    fun deleteSession(
        practiceId: UUID,
        sessionId: UUID,
        memberId: Long,
    ) {
        val practice = getPractice(practiceId)
        val session = getSessionByIdAndPractice(sessionId, practice)
        session.markAsDeleted(memberId)
    }

    @Transactional
    fun addParticipant(
        practiceId: UUID,
        request: PracticeMemberAddRequest,
    ): PracticeParticipantResponse {
        val practice = getPractice(practiceId)
        validateParticipantNotExists(practice, request.memberId)
        val participant =
            practiceParticipantRepository.save(
                PracticeParticipant.create(
                    practice = practice,
                    member = request.memberId,
                ),
            )
        return PracticeParticipantResponse.of(participant)
    }

    @Transactional
    fun assignSessionParticipant(
        practiceId: UUID,
        sessionId: UUID,
        memberId: Long,
    ) {
        val practice = getPractice(practiceId)
        val session = getSessionByIdAndPractice(sessionId, practice)
        if (session.participant != null) throw BusinessException(ErrorCode.PRACTICE_SESSION_ALREADY_ASSIGNED)
        val participant = getParticipant(practice, memberId)
        session.assignParticipant(participant)
    }

    @Transactional
    fun withdrawSessionParticipant(
        practiceId: UUID,
        sessionId: UUID,
        memberId: Long,
    ) {
        val practice = getPractice(practiceId)
        val session = getSessionByIdAndPractice(sessionId, practice)
        validateSessionAssignedToMember(session, memberId)
        session.withdrawParticipant()
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

    @Transactional
    fun upsertPracticeSongRefLink(
        songId: UUID,
        request: PracticeSongRefLinkUpsertRequest,
    ) {
        val song = getPracticeSong(songId)
        song.updateRefLink(request.refLink)
    }

    @Transactional
    fun deletePracticeSongRefLink(songId: UUID) {
        val song = getPracticeSong(songId)
        song.deleteRefLink()
    }

    // TODO: 외부 음원 검색 API / gRPC 연동으로 대체. 현재는 Tool 곡 mock 결과를 무조건 반환.
    fun searchSongs(keyword: String): List<Song> =
        listOf(
            Song(
                title = "Vicarious",
                artist = "Tool",
                album = "10,000 Days",
                duration = 426,
            ),
            Song(
                title = "Schism",
                artist = "Tool",
                album = "Lateralus",
                duration = 407,
            ),
        )

    @Transactional
    fun createPracticeSong(
        practiceId: UUID?,
        song: Song,
    ): PracticeSongResponse {
        val newSong =
            practiceSongRepository.save(
                PracticeSong.create(
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    duration = song.duration,
                    refLink = song.refLink,
                ),
            )
        practiceId?.let {
            val practice = getPractice(it)
            practice.updateSong(newSong)
        }
        return PracticeSongResponse.of(newSong)
    }

    @Transactional
    fun createPracticeSong(
        practiceId: UUID?,
        title: String,
        artist: String,
        album: String,
        duration: Int,
        refLink: String?,
    ): PracticeSongResponse =
        createPracticeSong(
            practiceId = practiceId,
            song =
                Song(
                    title = title,
                    artist = artist,
                    album = album,
                    duration = duration,
                    refLink = refLink,
                ),
        )

    @Transactional
    fun updatePracticeSong(
        songId: UUID,
        request: PracticeSongUpdateRequest,
    ): PracticeSongResponse {
        val song = getPracticeSong(songId)
        request.title?.let { song.updateTitle(it) }
        request.artist?.let { song.updateArtist(it) }
        request.album?.let { song.updateAlbum(it) }
        request.duration?.let { song.updateDuration(it) }
        request.refLink?.let { song.updateRefLink(it) }
        return PracticeSongResponse.of(song)
    }

    @Transactional
    fun upsertPracticeSong(
        songId: UUID,
        song: Song,
    ): PracticeSongResponse {
        val practiceSong = getPracticeSong(songId)
        practiceSong.updateAll(
            title = song.title,
            artist = song.artist,
            album = song.album,
            duration = song.duration,
            refLink = song.refLink,
        )
        return PracticeSongResponse.of(practiceSong)
    }

    // --- 내부 유틸리티 메서드 ---
    private fun getPractice(practiceId: UUID): Practice =
        practiceRepository.findByIdOrNull(practiceId)
            ?: throw BusinessException(ErrorCode.PRACTICE_NOT_FOUND)

    private fun getPracticeSong(songId: UUID): PracticeSong =
        practiceSongRepository.getPracticeSongById(songId)
            ?: throw BusinessException(ErrorCode.PRACTICE_SONG_NOT_FOUND)

    private fun getSessionByIdAndPractice(
        sessionId: UUID,
        practice: Practice,
    ): PracticeSession =
        practiceSessionRepository.findByIdAndPractice(sessionId, practice)
            ?: throw BusinessException(ErrorCode.PRACTICE_SESSION_NOT_FOUND)

    private fun getParticipant(
        practice: Practice,
        memberId: Long,
    ): PracticeParticipant =
        practiceParticipantRepository.findByPracticeAndMember(practice, memberId)
            ?: throw BusinessException(ErrorCode.PRACTICE_PARTICIPANT_NOT_FOUND)

    private fun validateParticipantNotExists(
        practice: Practice,
        memberId: Long,
    ) {
        if (practiceParticipantRepository.existsByPracticeAndMember(practice, memberId)) {
            throw BusinessException(ErrorCode.PRACTICE_PARTICIPANT_ALREADY_EXISTS)
        }
    }

    private fun validateSessionAssignedToMember(
        session: PracticeSession,
        memberId: Long,
    ) {
        if (session.participant?.member != memberId) {
            throw BusinessException(ErrorCode.PRACTICE_PARTICIPANT_NOT_FOUND)
        }
    }
}
