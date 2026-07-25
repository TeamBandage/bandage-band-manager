package com.bandage.bandmanager.domain.setlist.service

import com.bandage.bandmanager.domain.band.model.BandMember
import com.bandage.bandmanager.domain.band.repository.BandMemberRepository
import com.bandage.bandmanager.domain.member.service.MemberService
import com.bandage.bandmanager.domain.setlist.dto.req.SetlistPagingQuery
import com.bandage.bandmanager.domain.setlist.dto.req.SetlistTrackPagingQuery
import com.bandage.bandmanager.domain.setlist.dto.req.SetlistTrackUpdateRequest
import com.bandage.bandmanager.domain.setlist.dto.req.SetlistUpdateRequest
import com.bandage.bandmanager.domain.setlist.dto.res.SetlistDetailResponse
import com.bandage.bandmanager.domain.setlist.dto.res.SetlistResponse
import com.bandage.bandmanager.domain.setlist.dto.res.SetlistTrackResponse
import com.bandage.bandmanager.domain.setlist.model.Setlist
import com.bandage.bandmanager.domain.setlist.model.SetlistBand
import com.bandage.bandmanager.domain.setlist.model.SetlistTrack
import com.bandage.bandmanager.domain.setlist.model.SetlistTrackParticipant
import com.bandage.bandmanager.domain.setlist.repository.SetlistBandRepository
import com.bandage.bandmanager.domain.setlist.repository.SetlistRepository
import com.bandage.bandmanager.domain.setlist.repository.SetlistTrackParticipantRepository
import com.bandage.bandmanager.domain.setlist.repository.SetlistTrackRepository
import com.bandage.bandmanager.global.authority.MemberAuthorityCleanupHandler
import com.bandage.bandmanager.global.authority.ResourceAuthorityType
import com.bandage.bandmanager.global.authority.SuccessorSelector
import com.bandage.bandmanager.global.common.response.CursorResponse
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
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
    private val bandMemberRepository: BandMemberRepository,
    private val memberService: MemberService,
) : MemberAuthorityCleanupHandler {
    override val authorityType: ResourceAuthorityType = ResourceAuthorityType.SETLIST_MANAGEMENT

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
        request.managerId?.let { newManagerId ->
            if (newManagerId == memberId) throw BusinessException(ErrorCode.NO_CHANGE)
            if (!setlistRepository.isAccessibleMember(setlist.id, newManagerId)) {
                throw BusinessException(ErrorCode.SETLIST_MANAGER_NOT_PARTICIPANT)
            }
            setlist.changeManager(newManagerId)
        }
        return SetlistDetailResponse.of(setlist, loadBandIds(setlist.id))
    }

    /** 셋리스트 소프트 삭제. 트랙·참여자·밴드 연결도 함께 정리한다. */
    @Transactional
    fun deleteSetlist(
        setlistId: UUID,
        memberId: Long,
    ) {
        val setlist = getSetlistOrThrow(setlistId)
        validateManager(setlist, memberId)

        val tracks = setlistTrackRepository.findAllBySetlist(setlist)
        if (tracks.isNotEmpty()) {
            setlistTrackParticipantRepository.findAllByTrackIn(tracks).forEach { it.markAsDeleted(memberId) }
            tracks.forEach { it.markAsDeleted(memberId) }
        }
        setlistBandRepository.findAllBySetlistId(setlistId).forEach { it.markAsDeleted(memberId) }
        setlist.markAsDeleted(memberId)
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
        val allParticipants = setlistTrackParticipantRepository.findAllByTrackIn(result.content)
        val participantsByTrack = allParticipants.groupBy { it.track.id }
        // 페이지 전체 참여자 회원 정보를 1회 bulk 조회(목록 단위 N+1 방지)
        val memberInfos = memberService.getMemberSummaries(allParticipants.map { it.memberId })
        val content =
            result.content.map { track ->
                SetlistTrackResponse.of(
                    track = track,
                    participants = participantsByTrack[track.id] ?: emptyList(),
                    memberInfos = memberInfos,
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
        return toTrackResponse(track)
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

        track.updateMeta(request.title, request.artist, request.album, request.duration, request.reference, request.note)
        request.sessions?.let { sessions ->
            val newDefs = sessions.map { it.toEntity() }
            val newSessionIds = newDefs.map { it.sessionId }.toSet()
            setlistTrackParticipantRepository
                .findAllByTrack(track)
                .filter { it.sessionId !in newSessionIds }
                .forEach { setlistTrackParticipantRepository.delete(it) }
            track.replaceSessions(newDefs)
        }
        return toTrackResponse(track)
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

    /** 단건 트랙 응답 구성. 참여자 회원 정보를 1회 bulk 조회(N+1 방지). */
    private fun toTrackResponse(track: SetlistTrack): SetlistTrackResponse {
        val participants = setlistTrackParticipantRepository.findAllByTrack(track)
        val memberInfos = memberService.getMemberSummaries(participants.map { it.memberId })
        return SetlistTrackResponse.of(track, participants, memberInfos)
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

    /**
     * 회원 탈퇴 시 호출. 회원이 매니저인 모든 셋리스트의 매니저 권한을 자동 양도한다.
     * - 티어1: 셋리스트 참여자(SetlistTrackParticipant) 중 최고참
     * - 티어2: 연결된 밴드(SetlistBand)의 일반 멤버 중 최고참(밴드 등록 순)
     * - 후보 전무: 셋리스트 소프트 삭제
     * 후임 산정에 필요한 트랙/참여자/밴드/밴드멤버를 모두 일괄 조회해 추가 쿼리를 피한다.
     */
    @Transactional
    override fun cleanupOnWithdrawal(memberId: Long) {
        val managed = setlistRepository.findAllByManagerId(memberId)
        if (managed.isEmpty()) return

        val setlistIds = managed.map { it.id }
        val tracks = setlistTrackRepository.findAllBySetlistIdIn(setlistIds)
        val setlistIdByTrackId = tracks.associate { it.id to it.setlist.id }
        val participantsBySetlist =
            if (tracks.isEmpty()) {
                emptyMap()
            } else {
                setlistTrackParticipantRepository
                    .findAllByTrackIn(tracks)
                    .groupBy { setlistIdByTrackId[it.track.id] }
            }
        val bandsBySetlist = setlistBandRepository.findAllBySetlistIdIn(setlistIds).groupBy { it.setlistId }
        val allBandIds =
            bandsBySetlist.values
                .flatten()
                .map { it.bandId }
                .distinct()
        val membersByBand =
            if (allBandIds.isEmpty()) {
                emptyMap()
            } else {
                bandMemberRepository.findAllByBandIdIn(allBandIds).groupBy { it.band.id }
            }

        managed.forEach { setlist ->
            val successor =
                selectSetlistSuccessor(
                    participants = participantsBySetlist[setlist.id].orEmpty(),
                    bands = bandsBySetlist[setlist.id].orEmpty(),
                    membersByBand = membersByBand,
                    leavingMemberId = memberId,
                )
            if (successor != null) {
                setlist.changeManager(successor)
            } else {
                setlist.markAsDeleted(memberId)
            }
        }
    }

    private fun selectSetlistSuccessor(
        participants: List<SetlistTrackParticipant>,
        bands: List<SetlistBand>,
        membersByBand: Map<UUID, List<BandMember>>,
        leavingMemberId: Long,
    ): Long? {
        // 티어1: 셋리스트 참여자 중 최고참
        SuccessorSelector
            .oldestFromHighestTier(listOf(participants.filter { it.memberId != leavingMemberId })) { it.createdAt }
            ?.let { return it.memberId }

        // 티어2: 연결된 밴드의 일반 멤버 중 최고참(밴드 등록 순)
        val excluded = participants.mapTo(mutableSetOf()) { it.memberId }.apply { add(leavingMemberId) }
        val bandTiers =
            bands.sortedBy { it.createdAt }.map { it.bandId }.distinct().map { bandId ->
                membersByBand[bandId].orEmpty().filter { it.member !in excluded }
            }
        return SuccessorSelector.oldestFromHighestTier(bandTiers) { it.createdAt }?.member
    }
}
