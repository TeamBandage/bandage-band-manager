package com.bandage.bandmanager.domain.setlist.service

import com.bandage.bandmanager.domain.band.model.BandMember
import com.bandage.bandmanager.domain.band.repository.BandMemberRepository
import com.bandage.bandmanager.domain.member.dto.res.MemberSummary
import com.bandage.bandmanager.domain.member.service.MemberService
import com.bandage.bandmanager.domain.performance.repository.PerformanceSetlistRepository
import com.bandage.bandmanager.domain.setlist.dto.req.SetlistManagerTransferRequest
import com.bandage.bandmanager.domain.setlist.dto.req.SetlistPagingQuery
import com.bandage.bandmanager.domain.setlist.dto.req.SetlistParticipantPagingQuery
import com.bandage.bandmanager.domain.setlist.dto.req.SetlistTitleSearchQuery
import com.bandage.bandmanager.domain.setlist.dto.req.SetlistTrackPagingQuery
import com.bandage.bandmanager.domain.setlist.dto.req.SetlistTrackUpdateRequest
import com.bandage.bandmanager.domain.setlist.dto.req.SetlistUpdateRequest
import com.bandage.bandmanager.domain.setlist.dto.res.SetlistDetailResponse
import com.bandage.bandmanager.domain.setlist.dto.res.SetlistParticipantResponse
import com.bandage.bandmanager.domain.setlist.dto.res.SetlistParticipantSessionResponse
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
import com.bandage.bandmanager.global.common.domain.SessionDef
import com.bandage.bandmanager.global.common.response.CursorResponse
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import org.springframework.data.domain.PageRequest
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
    private val performanceSetlistRepository: PerformanceSetlistRepository,
    private val memberService: MemberService,
) : MemberAuthorityCleanupHandler {
    override val authorityType: ResourceAuthorityType = ResourceAuthorityType.SETLIST_MANAGEMENT

    fun getSetlist(
        setlistId: UUID,
        memberId: Long,
    ): SetlistDetailResponse {
        val setlist = getSetlistOrThrow(setlistId)
        validateAccess(setlist, memberId)
        return SetlistDetailResponse.of(setlist, loadBandIds(setlist.id), loadParticipants(setlist))
    }

    /** 셋리스트 참여 멤버 전체 조회(BD-226). 매니저는 트랙 배정이 없어도 포함된다. */
    fun getParticipants(
        setlistId: UUID,
        memberId: Long,
        query: SetlistParticipantPagingQuery,
    ): CursorResponse<SetlistParticipantResponse, Long> {
        val setlist = getSetlistOrThrow(setlistId)
        validateAccess(setlist, memberId)

        // 참여 회원 ID 를 먼저 페이징하고(회원 ID 오름차순), 그 페이지의 회원에 대해서만 세션 배정을 조립한다.
        // 매니저는 트랙 배정이 없어도 목록에 포함해야 하므로 커서보다 큰 경우 후보에 넣고 함께 정렬한다.
        val lastId = query.lastId
        val participantIds =
            setlistTrackParticipantRepository.findMemberIdsBySetlistIdAfter(
                setlistId,
                lastId ?: 0L,
                PageRequest.of(0, query.pageSize + 1),
            )
        val candidates = if (lastId == null || setlist.managerId > lastId) participantIds + setlist.managerId else participantIds
        val idPage = CursorResponse.of(candidates.distinct().sorted().take(query.pageSize + 1), query.pageSize) { it }

        return CursorResponse(loadParticipantsOf(setlist, idPage.content), idPage.nextCursor, idPage.hasNext)
    }

    fun getSetlistsByTitle(
        query: SetlistTitleSearchQuery,
        memberId: Long,
    ): CursorResponse<SetlistResponse, UUID> =
        setlistRepository
            .findAllAccessibleByTitleAndPaging(query.title, memberId, query.lastId, query.pageSize)
            .map { SetlistResponse.of(it) }

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
        return SetlistDetailResponse.of(setlist, loadBandIds(setlist.id), loadParticipants(setlist))
    }

    /** 매니저 권한 양도. 대상은 셋리스트 접근 가능 멤버여야 하며, 본인에게 양도할 수는 없다(BD-225). */
    @Transactional
    fun transferManager(
        setlistId: UUID,
        memberId: Long,
        request: SetlistManagerTransferRequest,
    ): SetlistDetailResponse {
        val setlist = getSetlistOrThrow(setlistId)
        validateManager(setlist, memberId)
        val newManagerId = request.managerId
        if (newManagerId == memberId) throw BusinessException(ErrorCode.NO_CHANGE)
        if (!setlistRepository.isAccessibleMember(setlist.id, newManagerId)) {
            throw BusinessException(ErrorCode.SETLIST_MANAGER_NOT_PARTICIPANT)
        }
        setlist.changeManager(newManagerId)
        return SetlistDetailResponse.of(setlist, loadBandIds(setlist.id), loadParticipants(setlist))
    }

    /**
     * 셋리스트 소프트 삭제. 트랙·참여자·밴드 연결도 함께 정리한다.
     * 공연에 연결된(PerformanceSetlist 가 존재하는) 셋리스트는 삭제할 수 없다.
     */
    @Transactional
    fun deleteSetlist(
        setlistId: UUID,
        memberId: Long,
    ) {
        val setlist = getSetlistOrThrow(setlistId)
        validateManager(setlist, memberId)
        if (performanceSetlistRepository.existsBySetlistId(setlistId)) {
            throw BusinessException(ErrorCode.SETLIST_REFERENCED_BY_PERFORMANCE)
        }

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

    /**
     * 여러 셋리스트의 트랙을 셋리스트별로 묶어 일괄 조립한다(BD-264).
     *
     * **접근 권한을 검사하지 않는다.** 호출자가 이미 권한을 검증했다는 전제이므로
     * 컨트롤러에서 직접 호출하지 말고, 권한 판정 주체(예: PerformanceSetlistTrackFacade 의
     * 공연 참여자 검증)를 거친 파사드에서만 호출한다.
     *
     * 트랙·참여자·회원 정보를 각각 1회씩만 조회한다(셋리스트 수와 무관하게 쿼리 3회).
     */
    fun getTracksBySetlistIds(setlistIds: Collection<UUID>): Map<UUID, List<SetlistTrackResponse>> {
        if (setlistIds.isEmpty()) return emptyMap()

        val tracks = setlistTrackRepository.findAllBySetlistIdIn(setlistIds)
        if (tracks.isEmpty()) return emptyMap()

        val participantsByTrack = setlistTrackParticipantRepository.findAllByTrackIn(tracks).groupBy { it.track.id }
        val memberInfos = memberService.getMemberSummaries(participantsByTrack.values.flatten().map { it.memberId })

        return tracks
            .sortedBy { it.id }
            .groupBy({ it.setlist.id }) { track ->
                SetlistTrackResponse.of(
                    track = track,
                    participants = participantsByTrack[track.id] ?: emptyList(),
                    memberInfos = memberInfos,
                )
            }
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
            val newDefs = SessionDef.createAll(sessions.map { it.toSpec() }, track.sessions.map { it.sessionId }.toSet())
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

    /**
     * 여러 셋리스트의 참여자를 셋리스트별로 묶어 일괄 조립한다(BD-264).
     *
     * **접근 권한을 검사하지 않는다.** 호출자가 이미 권한을 검증했다는 전제이므로
     * [getTracksBySetlistIds] 와 마찬가지로 파사드에서만 호출한다.
     *
     * 참여 정의는 셋리스트 단위 [loadParticipants] 와 동일하게 "그 셋리스트의 매니저 OR 트랙 참여자" 다.
     * 공연 참여자라는 이유로 다른 셋리스트의 참여자 목록에 섞여 들어가지 않는다(트랙 배정 없는 인원이
     * sessions:[] 로 노출되는 것을 막는다).
     *
     * 트랙·참여자·회원 정보를 각각 1회씩만 조회한다(셋리스트 수와 무관하게 쿼리 3회).
     */
    fun getParticipantsBySetlistIds(setlists: Collection<Setlist>): Map<UUID, List<SetlistParticipantResponse>> {
        if (setlists.isEmpty()) return emptyMap()

        val tracks = setlistTrackRepository.findAllBySetlistIdIn(setlists.map { it.id })
        val participants = if (tracks.isEmpty()) emptyList() else setlistTrackParticipantRepository.findAllByTrackIn(tracks)
        val memberInfos = memberService.getMemberSummaries(participants.map { it.memberId } + setlists.map { it.managerId })

        val sessionDefs = tracks.flatMap { track -> track.sessions.map { (track.id to it.sessionId) to it } }.toMap()
        val setlistIdByTrackId = tracks.associate { it.id to it.setlist.id }
        val participantsBySetlist = participants.groupBy { setlistIdByTrackId[it.track.id] }

        return setlists.associate { setlist ->
            val setlistParticipants = participantsBySetlist[setlist.id].orEmpty()
            setlist.id to
                buildParticipants(
                    setlist,
                    participantMemberIds(setlist, setlistParticipants),
                    setlistParticipants,
                    sessionDefs,
                    memberInfos,
                )
        }
    }

    private fun loadBandIds(setlistId: UUID): List<UUID> = setlistBandRepository.findAllBySetlistId(setlistId).map { it.bandId }

    /**
     * 셋리스트 참여자 목록을 조립한다(BD-226, BD-180).
     *
     * 참여 정의는 접근 권한 판정(SetlistRepositoryImpl.isAccessibleMember)과 동일하게
     * "매니저 OR 트랙 참여자" 로 맞춘다. 어긋나면 목록과 403 판정이 불일치한다.
     * 세션 이름/약어는 트랙의 SessionDef 에 있으므로 (trackId, sessionId) 로 매핑한다.
     * 회원 정보는 1회 bulk 조회한다(N+1 방지).
     */
    private fun loadParticipants(setlist: Setlist): List<SetlistParticipantResponse> {
        val tracks = setlistTrackRepository.findAllBySetlist(setlist)
        val participants = if (tracks.isEmpty()) emptyList() else setlistTrackParticipantRepository.findAllByTrackIn(tracks)
        val memberInfos = memberService.getMemberSummaries(participants.map { it.memberId } + setlist.managerId)
        val sessionDefs =
            tracks.flatMap { track -> track.sessions.map { (track.id to it.sessionId) to it } }.toMap()

        return buildParticipants(setlist, participantMemberIds(setlist, participants), participants, sessionDefs, memberInfos)
    }

    /** 페이징된 참여 회원 ID 에 대해서만 세션 배정을 조립한다(BD-286). 세션 정의를 위해 트랙은 셋리스트 단위로 조회한다. */
    private fun loadParticipantsOf(
        setlist: Setlist,
        memberIds: List<Long>,
    ): List<SetlistParticipantResponse> {
        if (memberIds.isEmpty()) return emptyList()
        val tracks = setlistTrackRepository.findAllBySetlist(setlist)
        val participants =
            if (tracks.isEmpty()) emptyList() else setlistTrackParticipantRepository.findAllByTrackInAndMemberIdIn(tracks, memberIds)
        val memberInfos = memberService.getMemberSummaries(memberIds)
        val sessionDefs =
            tracks.flatMap { track -> track.sessions.map { (track.id to it.sessionId) to it } }.toMap()

        return buildParticipants(setlist, memberIds, participants, sessionDefs, memberInfos)
    }

    /** 매니저는 트랙 배정이 없어도 참여자 목록에 포함한다(접근 권한 정의와 일치). 매니저를 맨 앞에 둔다. */
    private fun participantMemberIds(
        setlist: Setlist,
        participants: List<SetlistTrackParticipant>,
    ): List<Long> = (listOf(setlist.managerId) + participants.map { it.memberId }).distinct()

    /** 참여자 응답 조립. 셋리스트 단위/배치 조회가 동일한 참여 정의를 쓰도록 한 곳에 모은다. */
    private fun buildParticipants(
        setlist: Setlist,
        memberIds: List<Long>,
        participants: List<SetlistTrackParticipant>,
        sessionDefs: Map<Pair<UUID, String>, SessionDef>,
        memberInfos: Map<Long, MemberSummary>,
    ): List<SetlistParticipantResponse> {
        val bySession =
            participants.groupBy({ it.memberId }) { participant ->
                val def = sessionDefs[participant.track.id to participant.sessionId]
                SetlistParticipantSessionResponse(
                    setlistTrackId = participant.track.id,
                    sessionId = participant.sessionId,
                    // 세션 교체 후 남은 고아 배정이면 정의가 없다. 이 경우 토큰을 그대로 표시값으로 쓴다.
                    label = def?.label ?: participant.sessionId,
                    short = def?.short ?: participant.sessionId,
                )
            }

        return memberIds.map { id ->
            SetlistParticipantResponse(
                member = memberInfos[id],
                isManager = id == setlist.managerId,
                sessions = bySession[id].orEmpty(),
            )
        }
    }

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
