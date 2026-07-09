package com.bandage.bandmanager.domain.selection.service

import com.bandage.bandmanager.domain.band.repository.BandMemberRepository
import com.bandage.bandmanager.domain.member.dto.res.MemberSummary
import com.bandage.bandmanager.domain.member.service.MemberService
import com.bandage.bandmanager.domain.selection.dto.req.SetlistChatMessageCreateRequest
import com.bandage.bandmanager.domain.selection.dto.req.SetlistConfirmationUpdateRequest
import com.bandage.bandmanager.domain.selection.dto.req.SetlistParticipantsUpdateRequest
import com.bandage.bandmanager.domain.selection.dto.req.TrackSelectionCreateRequest
import com.bandage.bandmanager.domain.selection.dto.req.TrackSelectionItemCreateRequest
import com.bandage.bandmanager.domain.selection.dto.req.TrackSelectionItemPagingQuery
import com.bandage.bandmanager.domain.selection.dto.req.TrackSelectionItemSelectionRequest
import com.bandage.bandmanager.domain.selection.dto.req.TrackSelectionItemUpdateRequest
import com.bandage.bandmanager.domain.selection.dto.req.TrackSelectionPagingQuery
import com.bandage.bandmanager.domain.selection.dto.req.TrackSelectionUpdateRequest
import com.bandage.bandmanager.domain.selection.dto.res.SetlistChatMessageResponse
import com.bandage.bandmanager.domain.selection.dto.res.TrackSelectionDetailResponse
import com.bandage.bandmanager.domain.selection.dto.res.TrackSelectionItemResponse
import com.bandage.bandmanager.domain.selection.dto.res.TrackSelectionResponse
import com.bandage.bandmanager.domain.selection.model.PracticeWindow
import com.bandage.bandmanager.domain.selection.model.TrackSelection
import com.bandage.bandmanager.domain.selection.model.TrackSelectionBand
import com.bandage.bandmanager.domain.selection.model.TrackSelectionItem
import com.bandage.bandmanager.domain.selection.model.TrackSelectionItemApplicant
import com.bandage.bandmanager.domain.selection.model.TrackSelectionItemChatMessage
import com.bandage.bandmanager.domain.selection.model.TrackSelectionItemConfirmation
import com.bandage.bandmanager.domain.selection.model.TrackSelectionMember
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionBandRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionItemApplicantRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionItemChatMessageRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionItemConfirmationRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionItemRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionMemberRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionRepository
import com.bandage.bandmanager.global.authority.MemberAuthorityCleanupHandler
import com.bandage.bandmanager.global.authority.ResourceAuthorityType
import com.bandage.bandmanager.global.authority.SuccessorSelector
import com.bandage.bandmanager.global.common.domain.TrackInfo
import com.bandage.bandmanager.global.common.response.CursorResponse
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class TrackSelectionService(
    private val selectionRepository: TrackSelectionRepository,
    private val selectionMemberRepository: TrackSelectionMemberRepository,
    private val selectionBandRepository: TrackSelectionBandRepository,
    private val itemRepository: TrackSelectionItemRepository,
    private val applicantRepository: TrackSelectionItemApplicantRepository,
    private val confirmationRepository: TrackSelectionItemConfirmationRepository,
    private val chatMessageRepository: TrackSelectionItemChatMessageRepository,
    private val bandMemberRepository: BandMemberRepository,
    private val memberService: MemberService,
) : MemberAuthorityCleanupHandler {
    override val authorityType: ResourceAuthorityType = ResourceAuthorityType.TRACK_SELECTION_MANAGEMENT

    @Transactional
    fun createSelection(
        memberId: Long,
        request: TrackSelectionCreateRequest,
    ): TrackSelectionResponse {
        val practiceWindow = resolvePracticeWindow(request)
        val participantIds = (request.participantUserIds + request.managerId + memberId).toSet()
        if (request.managerId !in participantIds) {
            throw BusinessException(ErrorCode.SETLIST_MANAGER_NOT_PARTICIPANT)
        }

        val selection =
            selectionRepository.save(
                TrackSelection.create(
                    title = request.title,
                    managerId = request.managerId,
                    practiceWindow = practiceWindow,
                ),
            )
        val bandIds = request.bandIds.toSet().toList()
        bandIds.forEach { bandId ->
            selectionBandRepository.save(TrackSelectionBand.create(selection = selection, bandId = bandId))
        }
        participantIds.forEach { uid ->
            selectionMemberRepository.save(TrackSelectionMember.create(selection = selection, memberId = uid))
        }
        return TrackSelectionResponse.of(selection, bandIds)
    }

    fun getSelection(
        selectionId: UUID,
        memberId: Long,
    ): TrackSelectionDetailResponse {
        val selection = getSelectionOrThrow(selectionId)
        val members = selectionMemberRepository.findAllBySelection(selection)
        validateAccess(selection, members, memberId)
        val bandIds = selectionBandRepository.findAllBySelection(selection).map { it.bandId }
        return TrackSelectionDetailResponse.of(selection, bandIds, members, memberInfosOf(members))
    }

    fun getMySelections(
        memberId: Long,
        query: TrackSelectionPagingQuery,
    ): CursorResponse<TrackSelectionResponse, UUID> {
        val result = selectionRepository.findAllByMemberAndPaging(memberId, query.lastId, query.pageSize)
        val bandIdsBySelectionId =
            if (result.content.isEmpty()) {
                emptyMap()
            } else {
                result.content.associate { selection ->
                    selection.id to selectionBandRepository.findAllBySelection(selection).map { it.bandId }
                }
            }
        return CursorResponse(
            content =
                result.content.map { selection ->
                    TrackSelectionResponse.of(selection, bandIdsBySelectionId[selection.id] ?: emptyList())
                },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    @Transactional
    fun updateSelection(
        selectionId: UUID,
        memberId: Long,
        request: TrackSelectionUpdateRequest,
    ): TrackSelectionResponse {
        val selection = getSelectionOrThrow(selectionId)
        validateManager(selection, memberId)
        request.title?.let { selection.updateTitle(it) }
        request.managerId?.let { newManagerId ->
            val members = selectionMemberRepository.findAllBySelection(selection)
            if (members.none { it.memberId == newManagerId }) {
                throw BusinessException(ErrorCode.SETLIST_MANAGER_NOT_PARTICIPANT)
            }
            selection.changeManager(newManagerId)
        }
        return TrackSelectionResponse.of(selection, loadBandIds(selection))
    }

    @Transactional
    fun deleteSelection(
        selectionId: UUID,
        memberId: Long,
    ) {
        val selection = getSelectionOrThrow(selectionId)
        validateManager(selection, memberId)
        selection.markAsDeleted(memberId)
    }

    @Transactional
    fun updateParticipants(
        selectionId: UUID,
        memberId: Long,
        request: SetlistParticipantsUpdateRequest,
    ): TrackSelectionDetailResponse {
        val selection = getSelectionOrThrow(selectionId)
        validateManager(selection, memberId)

        val addByMemberId = request.add.associateBy { it.memberId }
        val addIds = addByMemberId.keys
        val removeIds = request.remove.toSet()
        if (selection.managerId in removeIds) {
            throw BusinessException(ErrorCode.SETLIST_CANNOT_REMOVE_MANAGER)
        }
        val intersection = addIds intersect removeIds
        if (intersection.isNotEmpty()) {
            throw BusinessException(ErrorCode.SETLIST_MANAGER_NOT_PARTICIPANT)
        }

        if (removeIds.isNotEmpty()) {
            val items = itemRepository.findAllBySelection(selection)
            removeIds.forEach { uid ->
                selectionMemberRepository.findBySelectionAndMemberId(selection, uid)?.let { selectionMemberRepository.delete(it) }
                if (items.isNotEmpty()) {
                    applicantRepository.deleteAllByItemInAndMemberId(items, uid)
                    confirmationRepository.deleteAllByItemInAndMemberId(items, uid)
                }
            }
        }
        addByMemberId.forEach { (uid, dto) ->
            val bandIds = dto.bandIds.toSet()
            val existing = selectionMemberRepository.findBySelectionAndMemberId(selection, uid)
            if (existing == null) {
                selectionMemberRepository.save(
                    TrackSelectionMember.create(selection = selection, memberId = uid, bandIds = bandIds),
                )
            } else {
                existing.replaceBandIds(bandIds)
            }
        }

        val members = selectionMemberRepository.findAllBySelection(selection)
        return TrackSelectionDetailResponse.of(selection, loadBandIds(selection), members, memberInfosOf(members))
    }

    // -------- items --------
    @Transactional
    fun createItem(
        selectionId: UUID,
        memberId: Long,
        request: TrackSelectionItemCreateRequest,
    ): TrackSelectionItemResponse {
        val selection = getSelectionOrThrow(selectionId)
        validateAccess(selection, memberId)
        if (selection.isLocked) throw BusinessException(ErrorCode.SETLIST_MEETING_LOCKED)

        val item =
            itemRepository.save(
                TrackSelectionItem.create(
                    selection = selection,
                    trackInfo =
                        TrackInfo(
                            title = request.title,
                            artist = request.artist,
                            album = request.album,
                            duration = request.duration,
                            reference = request.reference,
                        ),
                    proposerId = memberId,
                    note = request.note,
                    sessions = request.sessions.map { it.toEntity() },
                ),
            )
        return toItemResponse(item, emptyList(), emptyList())
    }

    fun getItems(
        selectionId: UUID,
        memberId: Long,
        query: TrackSelectionItemPagingQuery,
    ): CursorResponse<TrackSelectionItemResponse, UUID> {
        val selection = getSelectionOrThrow(selectionId)
        validateAccess(selection, memberId)
        val result = itemRepository.findAllBySelectionAndPaging(selectionId, query.lastId, query.pageSize)
        if (result.content.isEmpty()) {
            return CursorResponse(content = emptyList(), nextCursor = null, hasNext = false)
        }
        val allApplicants = applicantRepository.findAllByItemIn(result.content)
        val allConfirmations = confirmationRepository.findAllByItemIn(result.content)
        val applicants = allApplicants.groupBy { it.item.id }
        val confirmations = allConfirmations.groupBy { it.item.id }
        // 페이지 전체의 회원 정보를 1회 bulk 조회(목록 단위 N+1 방지)
        val memberInfos =
            memberService.getMemberSummaries(
                buildSet {
                    result.content.forEach { add(it.proposerId) }
                    allApplicants.forEach { add(it.memberId) }
                    allConfirmations.forEach { add(it.memberId) }
                },
            )
        val content =
            result.content.map { item ->
                TrackSelectionItemResponse.of(
                    item = item,
                    applicants = applicants[item.id] ?: emptyList(),
                    confirmations = confirmations[item.id] ?: emptyList(),
                    memberInfos = memberInfos,
                )
            }
        return CursorResponse(content = content, nextCursor = result.nextCursor, hasNext = result.hasNext)
    }

    fun getItem(
        selectionId: UUID,
        itemId: UUID,
        memberId: Long,
    ): TrackSelectionItemResponse {
        val selection = getSelectionOrThrow(selectionId)
        validateAccess(selection, memberId)
        val item = getItemOrThrow(selection, itemId)
        return toItemResponse(
            item = item,
            applicants = applicantRepository.findAllByItem(item),
            confirmations = confirmationRepository.findAllByItem(item),
        )
    }

    @Transactional
    fun updateItem(
        selectionId: UUID,
        itemId: UUID,
        memberId: Long,
        request: TrackSelectionItemUpdateRequest,
    ): TrackSelectionItemResponse {
        val selection = getSelectionOrThrow(selectionId)
        validateAccess(selection, memberId)
        if (selection.isLocked) throw BusinessException(ErrorCode.SETLIST_MEETING_LOCKED)
        val item = getItemOrThrow(selection, itemId)
        if (item.proposerId != memberId && selection.managerId != memberId) {
            throw BusinessException(ErrorCode.SETLIST_MEETING_ITEM_FORBIDDEN)
        }
        item.updateMeta(request.title, request.artist, request.album, request.duration, request.reference, request.note)
        request.sessions?.let { sessions ->
            val newDefs = sessions.map { it.toEntity() }
            val newSessionIds = newDefs.map { it.sessionId }.toSet()
            val applicants = applicantRepository.findAllByItem(item)
            val confirmations = confirmationRepository.findAllByItem(item)
            applicants.filter { it.sessionId !in newSessionIds }.forEach { applicantRepository.delete(it) }
            confirmations.filter { it.sessionId !in newSessionIds }.forEach { confirmationRepository.delete(it) }
            item.replaceSessions(newDefs)
        }
        return toItemResponse(
            item = item,
            applicants = applicantRepository.findAllByItem(item),
            confirmations = confirmationRepository.findAllByItem(item),
        )
    }

    @Transactional
    fun deleteItem(
        selectionId: UUID,
        itemId: UUID,
        memberId: Long,
    ) {
        val selection = getSelectionOrThrow(selectionId)
        validateAccess(selection, memberId)
        if (selection.isLocked) throw BusinessException(ErrorCode.SETLIST_MEETING_LOCKED)
        val item = getItemOrThrow(selection, itemId)
        if (item.proposerId != memberId && selection.managerId != memberId) {
            throw BusinessException(ErrorCode.SETLIST_MEETING_ITEM_FORBIDDEN)
        }
        item.markAsDeleted(memberId)
    }

    @Transactional
    fun updateItemSelection(
        selectionId: UUID,
        itemId: UUID,
        memberId: Long,
        request: TrackSelectionItemSelectionRequest,
    ): TrackSelectionItemResponse {
        val selection = getSelectionOrThrow(selectionId)
        validateManager(selection, memberId)
        if (selection.isLocked) throw BusinessException(ErrorCode.SETLIST_MEETING_LOCKED)

        val item = getItemOrThrow(selection, itemId)
        if (request.selected) {
            validateAllSessionsConfirmed(item)
            item.select()
        } else {
            item.deselect()
        }
        return toItemResponse(
            item = item,
            applicants = applicantRepository.findAllByItem(item),
            confirmations = confirmationRepository.findAllByItem(item),
        )
    }

    private fun validateAllSessionsConfirmed(item: TrackSelectionItem) {
        item.sessions.forEach { sessionDef ->
            if (!confirmationRepository.existsByItemAndSessionId(item, sessionDef.sessionId)) {
                throw BusinessException(ErrorCode.SETLIST_SELECTION_INCOMPLETE_SESSION)
            }
        }
    }

    // -------- applicants --------
    @Transactional
    fun applyForSession(
        selectionId: UUID,
        itemId: UUID,
        sessionId: String,
        memberId: Long,
    ) {
        val selection = getSelectionOrThrow(selectionId)
        validateAccess(selection, memberId)
        val item = getItemOrThrow(selection, itemId)
        validateSessionExists(item, sessionId)
        if (applicantRepository.existsByItemAndSessionIdAndMemberId(item, sessionId, memberId)) return
        applicantRepository.save(TrackSelectionItemApplicant.create(item = item, sessionId = sessionId, memberId = memberId))
    }

    @Transactional
    fun withdrawSessionApplication(
        selectionId: UUID,
        itemId: UUID,
        sessionId: String,
        targetMemberId: Long,
        memberId: Long,
    ) {
        if (targetMemberId != memberId) {
            throw BusinessException(ErrorCode.SETLIST_MEETING_ITEM_FORBIDDEN)
        }
        val selection = getSelectionOrThrow(selectionId)
        validateAccess(selection, memberId)
        val item = getItemOrThrow(selection, itemId)
        applicantRepository.findByItemAndSessionIdAndMemberId(item, sessionId, memberId)?.let {
            applicantRepository.delete(it)
        }
        confirmationRepository.findByItemAndSessionIdAndMemberId(item, sessionId, memberId)?.let {
            confirmationRepository.delete(it)
        }
    }

    // -------- confirmations --------
    @Transactional
    fun updateConfirmations(
        selectionId: UUID,
        itemId: UUID,
        sessionId: String,
        memberId: Long,
        request: SetlistConfirmationUpdateRequest,
    ): TrackSelectionItemResponse {
        val selection = getSelectionOrThrow(selectionId)
        validateManager(selection, memberId)
        val item = getItemOrThrow(selection, itemId)
        validateSessionExists(item, sessionId)

        request.unconfirm.forEach { uid ->
            confirmationRepository.findByItemAndSessionIdAndMemberId(item, sessionId, uid)?.let {
                confirmationRepository.delete(it)
            }
        }
        val toAdd =
            request.confirm.filter { uid ->
                confirmationRepository.findByItemAndSessionIdAndMemberId(item, sessionId, uid) == null
            }
        if (toAdd.isNotEmpty() && confirmationRepository.existsByItemAndSessionId(item, sessionId)) {
            throw BusinessException(ErrorCode.SETLIST_MEETING_ITEM_SESSION_FULL)
        }
        toAdd.forEach { uid ->
            confirmationRepository.save(
                TrackSelectionItemConfirmation.create(item = item, sessionId = sessionId, memberId = uid, confirmedBy = memberId),
            )
        }
        return toItemResponse(
            item = item,
            applicants = applicantRepository.findAllByItem(item),
            confirmations = confirmationRepository.findAllByItem(item),
        )
    }

    // -------- chat --------
    fun getChatMessages(
        selectionId: UUID,
        itemId: UUID,
        memberId: Long,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<SetlistChatMessageResponse, UUID> {
        val selection = getSelectionOrThrow(selectionId)
        validateAccess(selection, memberId)
        val item = getItemOrThrow(selection, itemId)
        val result = chatMessageRepository.findAllByItemAndPaging(item.id, lastId, pageSize)
        // 페이지 내 작성자 회원 정보를 1회 bulk 조회(N+1 방지)
        val memberInfos = memberService.getMemberSummaries(result.content.map { it.memberId })
        return CursorResponse(
            content = result.content.map { SetlistChatMessageResponse.of(it, memberInfos[it.memberId]) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    @Transactional
    fun createChatMessage(
        selectionId: UUID,
        itemId: UUID,
        memberId: Long,
        request: SetlistChatMessageCreateRequest,
    ): SetlistChatMessageResponse {
        val selection = getSelectionOrThrow(selectionId)
        validateAccess(selection, memberId)
        val item = getItemOrThrow(selection, itemId)
        val msg =
            chatMessageRepository.save(
                TrackSelectionItemChatMessage.create(item = item, memberId = memberId, message = request.message),
            )
        return SetlistChatMessageResponse.of(msg, memberService.getMemberSummaries(listOf(memberId))[memberId])
    }

    // -------- lock / unlock --------
    @Transactional
    fun lockSelection(
        selectionId: UUID,
        memberId: Long,
    ): TrackSelectionResponse {
        val selection = getSelectionOrThrow(selectionId)
        validateManager(selection, memberId)
        if (selection.isLocked) throw BusinessException(ErrorCode.SETLIST_MEETING_LOCKED)
        selection.lock()
        return TrackSelectionResponse.of(selection, loadBandIds(selection))
    }

    @Transactional
    fun unlockSelection(
        selectionId: UUID,
        memberId: Long,
    ): TrackSelectionResponse {
        val selection = getSelectionOrThrow(selectionId)
        validateManager(selection, memberId)
        if (!selection.isLocked) throw BusinessException(ErrorCode.SETLIST_MEETING_NOT_LOCKED)
        selection.unlock()
        return TrackSelectionResponse.of(selection, loadBandIds(selection))
    }

    // -------- internal --------
    private fun getSelectionOrThrow(selectionId: UUID): TrackSelection =
        selectionRepository.findByIdOrNull(selectionId)
            ?: throw BusinessException(ErrorCode.SETLIST_MEETING_NOT_FOUND)

    private fun loadBandIds(selection: TrackSelection): List<UUID> = selectionBandRepository.findAllBySelection(selection).map { it.bandId }

    /** 참여자 회원 정보를 1회 bulk 조회(N+1 방지). */
    private fun memberInfosOf(members: List<TrackSelectionMember>): Map<Long, MemberSummary> =
        memberService.getMemberSummaries(members.map { it.memberId })

    /** 선곡 항목 응답에 필요한 회원(제안자/지원자/확정자) 정보를 1회 bulk 조회(N+1 방지). */
    private fun itemMemberInfos(
        item: TrackSelectionItem,
        applicants: List<TrackSelectionItemApplicant>,
        confirmations: List<TrackSelectionItemConfirmation>,
    ): Map<Long, MemberSummary> =
        memberService.getMemberSummaries(
            buildSet {
                add(item.proposerId)
                applicants.forEach { add(it.memberId) }
                confirmations.forEach { add(it.memberId) }
            },
        )

    private fun toItemResponse(
        item: TrackSelectionItem,
        applicants: List<TrackSelectionItemApplicant>,
        confirmations: List<TrackSelectionItemConfirmation>,
    ): TrackSelectionItemResponse =
        TrackSelectionItemResponse.of(item, applicants, confirmations, itemMemberInfos(item, applicants, confirmations))

    private fun getItemOrThrow(
        selection: TrackSelection,
        itemId: UUID,
    ): TrackSelectionItem {
        val item =
            itemRepository.findByIdOrNull(itemId)
                ?: throw BusinessException(ErrorCode.SETLIST_MEETING_ITEM_NOT_FOUND)
        if (item.selection.id != selection.id) throw BusinessException(ErrorCode.SETLIST_MEETING_ITEM_NOT_FOUND)
        return item
    }

    private fun validateAccess(
        selection: TrackSelection,
        memberId: Long,
    ) {
        if (selection.managerId == memberId) return
        if (!selectionMemberRepository.existsBySelectionAndMemberId(selection, memberId)) {
            throw BusinessException(ErrorCode.SETLIST_MEETING_FORBIDDEN)
        }
    }

    private fun validateAccess(
        selection: TrackSelection,
        members: List<TrackSelectionMember>,
        memberId: Long,
    ) {
        if (selection.managerId == memberId) return
        if (members.none { it.memberId == memberId }) {
            throw BusinessException(ErrorCode.SETLIST_MEETING_FORBIDDEN)
        }
    }

    private fun validateManager(
        selection: TrackSelection,
        memberId: Long,
    ) {
        if (selection.managerId != memberId) throw BusinessException(ErrorCode.SETLIST_MEETING_NOT_MANAGER)
    }

    private fun validateSessionExists(
        item: TrackSelectionItem,
        sessionId: String,
    ) {
        if (item.sessions.none { it.sessionId == sessionId }) {
            throw BusinessException(ErrorCode.SETLIST_MEETING_ITEM_SESSION_NOT_FOUND)
        }
    }

    private fun resolvePracticeWindow(request: TrackSelectionCreateRequest): PracticeWindow {
        val window =
            request.practiceWindow
                ?: throw BusinessException(ErrorCode.SETLIST_PRACTICE_WINDOW_REQUIRED)
        if (window.from.isAfter(window.to)) {
            throw BusinessException(ErrorCode.SETLIST_PRACTICE_WINDOW_INVALID)
        }
        return window.toEntity()
    }

    /**
     * 회원 탈퇴 시 호출. 회원이 매니저인 모든 선곡 회의의 매니저 권한을 자동 양도한다.
     * - 티어1: 회의 참여자(TrackSelectionMember) 중 최고참 (이미 참여자이므로 changeManager 만으로 충분)
     * - 티어2: 연결된 밴드(TrackSelectionBand)의 일반 멤버 중 최고참 → 신규 매니저를 참여자로도 등록
     *          (매니저는 참여자에 포함되어야 한다는 불변식 유지)
     * - 후보 전무: 선곡 회의 소프트 삭제 (잠긴 회의도 매니저가 사라지면 해소 불가하므로)
     * 후임 산정에 필요한 참여자/밴드/밴드멤버를 모두 일괄 조회해 추가 쿼리를 피한다.
     */
    @Transactional
    override fun cleanupOnWithdrawal(memberId: Long) {
        val managed = selectionRepository.findAllByManagerId(memberId)
        if (managed.isEmpty()) return

        val membersBySelection = selectionMemberRepository.findAllBySelectionIn(managed).groupBy { it.selection.id }
        val bandsBySelection = selectionBandRepository.findAllBySelectionIn(managed).groupBy { it.selection.id }
        val allBandIds =
            bandsBySelection.values
                .flatten()
                .map { it.bandId }
                .distinct()
        val membersByBand =
            if (allBandIds.isEmpty()) {
                emptyMap()
            } else {
                bandMemberRepository.findAllByBandIdIn(allBandIds).groupBy { it.band.id }
            }

        managed.forEach { selection ->
            val members = membersBySelection[selection.id].orEmpty()

            // 티어1: 회의 참여자 중 최고참
            SuccessorSelector
                .oldestFromHighestTier(listOf(members.filter { it.memberId != memberId })) { it.createdAt }
                ?.let {
                    selection.changeManager(it.memberId)
                    return@forEach
                }

            // 티어2: 연결된 밴드의 일반 멤버 중 최고참(밴드 등록 순) → 참여자로도 등록
            val excluded = members.mapTo(mutableSetOf()) { it.memberId }.apply { add(memberId) }
            val bandTiers =
                bandsBySelection[selection.id].orEmpty().sortedBy { it.createdAt }.map { it.bandId }.distinct().map { bandId ->
                    membersByBand[bandId].orEmpty().filter { it.member !in excluded }
                }
            val bandSuccessor = SuccessorSelector.oldestFromHighestTier(bandTiers) { it.createdAt }?.member
            if (bandSuccessor != null) {
                selection.changeManager(bandSuccessor)
                selectionMemberRepository.save(TrackSelectionMember.create(selection, bandSuccessor))
                return@forEach
            }

            // 후보 전무 → 선곡 회의 소프트 삭제
            selection.markAsDeleted(memberId)
        }
    }
}
