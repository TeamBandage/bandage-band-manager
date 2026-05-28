package com.bandage.v1.domain.setlist.service

import com.bandage.v1.domain.setlist.dto.req.SetlistPagingQuery
import com.bandage.v1.domain.setlist.dto.req.SetlistTrackPagingQuery
import com.bandage.v1.domain.setlist.dto.req.SetlistTrackUpdateRequest
import com.bandage.v1.domain.setlist.dto.req.SetlistUpdateRequest
import com.bandage.v1.domain.setlist.dto.res.SetlistDetailResponse
import com.bandage.v1.domain.setlist.dto.res.SetlistResponse
import com.bandage.v1.domain.setlist.dto.res.SetlistTrackResponse
import com.bandage.v1.domain.setlist.model.Setlist
import com.bandage.v1.domain.setlist.model.SetlistTrack
import com.bandage.v1.domain.setlist.repository.SetlistBandRepository
import com.bandage.v1.domain.setlist.repository.SetlistRepository
import com.bandage.v1.domain.setlist.repository.SetlistTrackParticipantRepository
import com.bandage.v1.domain.setlist.repository.SetlistTrackRepository
import com.bandage.v1.global.common.response.CursorResponse
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class SetlistService(
    private val setlistRepository: SetlistRepository,
    private val setlistBandRepository: SetlistBandRepository,
    private val setlistTrackRepository: SetlistTrackRepository,
    private val setlistTrackParticipantRepository: SetlistTrackParticipantRepository,
) {
    fun getSetlist(
        setlistId: UUID,
        memberId: Long,
    ): SetlistDetailResponse {
        val setlist = getSetlistOrThrow(setlistId)
        validateAccess(setlist, memberId)
        return SetlistDetailResponse.of(setlist, loadBandIds(setlist.id))
    }

    fun getSetlistsByTitle(
        title: String,
        memberId: Long,
    ): List<SetlistResponse> = setlistRepository.findAllAccessibleByTitle(title, memberId).map { SetlistResponse.of(it) }

    fun getMySetlists(
        memberId: Long,
        query: SetlistPagingQuery,
    ): CursorResponse<SetlistResponse, UUID> {
        val result = setlistRepository.findAllByMemberAndPaging(memberId, query.lastId, query.pageSize)
        return CursorResponse(
            content = result.content.map { SetlistResponse.of(it) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    @Transactional
    fun updateSetlist(
        setlistId: UUID,
        memberId: Long,
        request: SetlistUpdateRequest,
    ): SetlistDetailResponse {
        val setlist = getSetlistOrThrow(setlistId)
        validateManager(setlist, memberId)
        setlist.updateTitle(request.title)
        return SetlistDetailResponse.of(setlist, loadBandIds(setlist.id))
    }

    fun getTracks(
        setlistId: UUID,
        memberId: Long,
        query: SetlistTrackPagingQuery,
    ): CursorResponse<SetlistTrackResponse, UUID> {
        val setlist = getSetlistOrThrow(setlistId)
        validateAccess(setlist, memberId)
        val result = setlistTrackRepository.findAllBySetlistAndPaging(setlistId, query.lastId, query.pageSize)
        if (result.content.isEmpty()) {
            return CursorResponse(content = emptyList(), nextCursor = null, hasNext = false)
        }
        val participantsByTrack =
            setlistTrackParticipantRepository.findAllByTrackIn(result.content).groupBy { it.track.id }
        val content =
            result.content.map { track ->
                SetlistTrackResponse.of(
                    track = track,
                    participants = participantsByTrack[track.id] ?: emptyList(),
                )
            }
        return CursorResponse(content = content, nextCursor = result.nextCursor, hasNext = result.hasNext)
    }

    fun getTrack(
        setlistId: UUID,
        trackId: UUID,
        memberId: Long,
    ): SetlistTrackResponse {
        val setlist = getSetlistOrThrow(setlistId)
        validateAccess(setlist, memberId)
        val track = getTrackOrThrow(setlist, trackId)
        return SetlistTrackResponse.of(
            track = track,
            participants = setlistTrackParticipantRepository.findAllByTrack(track),
        )
    }

    @Transactional
    fun updateTrack(
        setlistId: UUID,
        trackId: UUID,
        memberId: Long,
        request: SetlistTrackUpdateRequest,
    ): SetlistTrackResponse {
        val setlist = getSetlistOrThrow(setlistId)
        validateManager(setlist, memberId)
        val track = getTrackOrThrow(setlist, trackId)

        track.updateMeta(request.title, request.artist, request.album, request.duration, request.note)
        request.sessions?.let { sessions ->
            val newDefs = sessions.map { it.toEntity() }
            val newSessionIds = newDefs.map { it.sessionId }.toSet()
            setlistTrackParticipantRepository
                .findAllByTrack(track)
                .filter { it.sessionId !in newSessionIds }
                .forEach { setlistTrackParticipantRepository.delete(it) }
            track.replaceSessions(newDefs)
        }
        return SetlistTrackResponse.of(
            track = track,
            participants = setlistTrackParticipantRepository.findAllByTrack(track),
        )
    }

    @Transactional
    fun deleteTrack(
        setlistId: UUID,
        trackId: UUID,
        memberId: Long,
    ) {
        val setlist = getSetlistOrThrow(setlistId)
        validateManager(setlist, memberId)
        val track = getTrackOrThrow(setlist, trackId)
        setlistTrackParticipantRepository.findAllByTrack(track).forEach { it.markAsDeleted(memberId) }
        track.markAsDeleted(memberId)
    }

    private fun getSetlistOrThrow(setlistId: UUID): Setlist =
        setlistRepository.findByIdOrNull(setlistId)
            ?: throw BusinessException(ErrorCode.SETLIST_NOT_FOUND)

    private fun getTrackOrThrow(
        setlist: Setlist,
        trackId: UUID,
    ): SetlistTrack {
        val track =
            setlistTrackRepository.findByIdOrNull(trackId)
                ?: throw BusinessException(ErrorCode.SETLIST_TRACK_NOT_FOUND)
        if (track.setlist.id != setlist.id) throw BusinessException(ErrorCode.SETLIST_TRACK_NOT_FOUND)
        return track
    }

    private fun loadBandIds(setlistId: UUID): List<UUID> = setlistBandRepository.findAllBySetlistId(setlistId).map { it.bandId }

    private fun validateAccess(
        setlist: Setlist,
        memberId: Long,
    ) {
        if (setlist.managerId == memberId) return
        if (!setlistRepository.isAccessibleMember(setlist.id, memberId)) {
            throw BusinessException(ErrorCode.SETLIST_FORBIDDEN)
        }
    }

    private fun validateManager(
        setlist: Setlist,
        memberId: Long,
    ) {
        if (setlist.managerId != memberId) throw BusinessException(ErrorCode.SETLIST_NOT_MANAGER)
    }
}
