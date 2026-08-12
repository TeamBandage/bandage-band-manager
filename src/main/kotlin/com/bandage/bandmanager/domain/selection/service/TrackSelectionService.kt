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
import com.bandage.bandmanager.domain.selection.dto.req.TrackSelectionManagerTransferRequest
import com.bandage.bandmanager.domain.selection.dto.req.TrackSelectionPagingQuery
import com.bandage.bandmanager.domain.selection.dto.req.TrackSelectionUpdateRequest
import com.bandage.bandmanager.domain.selection.dto.res.SetlistChatMessageResponse
import com.bandage.bandmanager.domain.selection.dto.res.TrackSelectionDetailResponse
import com.bandage.bandmanager.domain.selection.dto.res.TrackSelectionItemResponse
import com.bandage.bandmanager.domain.selection.dto.res.TrackSelectionResponse
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
import com.bandage.bandmanager.global.common.domain.SessionDef
import com.bandage.bandmanager.global.common.domain.TrackInfo
import com.bandage.bandmanager.global.common.response.CursorResponse
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import com.bandage.bandmanager.global.notify.annotation.Notify
import com.bandage.bandmanager.global.notify.annotation.NotifyCategory
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
        val participantIds = (request.participantUserIds + request.managerId + memberId).toSet()
        if (request.managerId !in participantIds) {
            throw BusinessException(ErrorCode.SETLIST_MANAGER_NOT_PARTICIPANT)
        }

        val selection =
            selectionRepository.save(
                TrackSelection.create(
                    title = request.title,
                    managerId = request.managerId,
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
        return TrackSelectionResponse.of(selection, loadBandIds(selection))
    }

    /** 매니저 권한 양도. 대상은 회의 참여자여야 하며, 본인에게 양도할 수는 없다(BD-225). */
    @Transactional
    fun transferManager(
        selectionId: UUID,
        memberId: Long,
        request: TrackSelectionManagerTransferRequest,
    ): TrackSelectionResponse {
        val selection = getSelectionOrThrow(selectionId)
        validateManager(selection, memberId)
        val newManagerId = request.managerId
        if (newManagerId == memberId) throw BusinessException(ErrorCode.NO_CHANGE)
        val members = selectionMemberRepository.findAllBySelection(selection)
        if (members.none { it.memberId == newManagerId }) {
            throw BusinessException(ErrorCode.SETLIST_MANAGER_NOT_PARTICIPANT)
        }
        selection.changeManager(newManagerId)
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

    @Notify(NotifyCategory.SELECTION_PARTICIPANT_ADDED)
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

    /**
     * 선곡 회의 떠나기(BD-218). 상세 정책: docs/TRACK-SELECTION-LEAVE.md
     *
     * 처리 순서
     *  1. 떠나는 멤버가 매니저면 기존 후임 선정 정책으로 권한 양도. 후보가 없으면 회의를 소프트 삭제하고 종료.
     *  2. 해당 멤버의 세션 지원/확정 연결을 모두 삭제.
     *  3. 지원/확정이 사라진 아이템은 선곡 확정 상태를 유지할 수 없으므로 isSelected = false.
     *  4. 해당 멤버가 제안한 아이템의 proposerId 를 null 로 해제.
     *  5. 참여자(TrackSelectionMember) 연결 삭제.
     */
    @Transactional
    fun leaveSelection(
        selectionId: UUID,
        memberId: Long,
    ) {
        val selection = getSelectionOrThrow(selectionId)
        val membership =
            selectionMemberRepository.findBySelectionAndMemberId(selection, memberId)
                ?: throw BusinessException(ErrorCode.SETLIST_MEETING_FORBIDDEN)

        val items = itemRepository.findAllBySelection(selection)

        if (selection.managerId == memberId) {
            val successor = selectSuccessor(selection, leavingMemberId = memberId)
            if (successor == null) {
                // 후임 후보가 없으면 회의 자체를 해소한다(매니저 없는 회의는 진행 불가).
                selection.markAsDeleted(memberId)
                return
            }
            selection.changeManager(successor)
        }

        if (items.isNotEmpty()) {
            // 이 멤버의 지원/확정이 걸려 있던 아이템만 선곡 확정을 해제한다(전체 일괄 해제가 아님).
            val affected =
                (
                    applicantRepository.findAllByItemIn(items).filter { it.memberId == memberId }.map { it.item.id } +
                        confirmationRepository.findAllByItemIn(items).filter { it.memberId == memberId }.map { it.item.id }
                ).toSet()
            applicantRepository.deleteAllByItemInAndMemberId(items, memberId)
            confirmationRepository.deleteAllByItemInAndMemberId(items, memberId)
            items.filter { it.id in affected }.forEach { it.deselect() }
            items.filter { it.proposerId == memberId }.forEach { it.clearProposer() }
        }

        selectionMemberRepository.delete(membership)
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
                    sessions = SessionDef.createAll(request.sessions.map { it.toSpec() }),
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
        val result =
            itemRepository.findAllBySelectionAndPaging(
                selectionId = selectionId,
                memberId = memberId,
                filter = query.toFilter(),
                lastId = query.lastId,
                pageSize = query.pageSize,
            )
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
                    result.content.forEach { item -> item.proposerId?.let { add(it) } }
                    allApplicants.forEach { add(it.memberId) }
                    allConfirmations.forEach { add(it.memberId) }
                },
            )
        val chatCounts = chatMessageCounts(result.content.map { it.id })
        val content =
            result.content.map { item ->
                TrackSelectionItemResponse.of(
                    item = item,
                    applicants = applicants[item.id] ?: emptyList(),
                    confirmations = confirmations[item.id] ?: emptyList(),
                    memberInfos = memberInfos,
                    chatMessageCount = chatCounts[item.id] ?: 0,
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
        // 통과 조건은 '매니저 OR 제안자'. 매니저를 먼저 비교해 proposerId 가 null(BD-218 떠남)인 경우에도 안전하다.
        if (selection.managerId != memberId && item.proposerId != memberId) {
            throw BusinessException(ErrorCode.SETLIST_MEETING_ITEM_FORBIDDEN)
        }
        item.updateMeta(request.title, request.artist, request.album, request.duration, request.reference, request.note)
        request.sessions?.let { sessions ->
            val newDefs = SessionDef.createAll(sessions.map { it.toSpec() }, item.sessions.map { it.sessionId }.toSet())
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
        // 통과 조건은 '매니저 OR 제안자'. 매니저를 먼저 비교해 proposerId 가 null(BD-218 떠남)인 경우에도 안전하다.
        if (selection.managerId != memberId && item.proposerId != memberId) {
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
                item.proposerId?.let { add(it) }
                applicants.forEach { add(it.memberId) }
                confirmations.forEach { add(it.memberId) }
            },
        )

    /** 페이징(cursor) 쿼리와 분리된 항목별 전체 채팅 수 집계. */
    private fun chatMessageCounts(itemIds: List<UUID>): Map<UUID, Long> {
        if (itemIds.isEmpty()) return emptyMap()
        return chatMessageRepository.countByItemIds(itemIds).associate { it.getItemId() to it.getMessageCount() }
    }

    private fun toItemResponse(
        item: TrackSelectionItem,
        applicants: List<TrackSelectionItemApplicant>,
        confirmations: List<TrackSelectionItemConfirmation>,
    ): TrackSelectionItemResponse =
        TrackSelectionItemResponse.of(
            item = item,
            applicants = applicants,
            confirmations = confirmations,
            memberInfos = itemMemberInfos(item, applicants, confirmations),
            chatMessageCount = chatMessageCounts(listOf(item.id))[item.id] ?: 0,
        )

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

    /**
     * 단일 선곡 회의의 매니저 후임을 선정한다(BD-218 떠나기 경로용).
     * 티어 정책은 [cleanupOnWithdrawal] 과 동일하며, 티어2 후임은 참여자로도 등록한다.
     * 다건 처리인 탈퇴 경로와 달리 회의 1건만 다루므로 벌크 조회 없이 직접 조회한다.
     */
    private fun selectSuccessor(
        selection: TrackSelection,
        leavingMemberId: Long,
    ): Long? {
        val members = selectionMemberRepository.findAllBySelection(selection)

        // 티어1: 회의 참여자 중 최고참
        SuccessorSelector
            .oldestFromHighestTier(listOf(members.filter { it.memberId != leavingMemberId })) { it.createdAt }
            ?.let { return it.memberId }

        // 티어2: 연결된 밴드의 일반 멤버 중 최고참(밴드 등록 순) → 참여자로도 등록
        val excluded = members.mapTo(mutableSetOf()) { it.memberId }.apply { add(leavingMemberId) }
        val bands =
            selectionBandRepository
                .findAllBySelection(selection)
                .sortedBy { it.createdAt }
                .map { it.bandId }
                .distinct()
        if (bands.isEmpty()) return null
        val membersByBand = bandMemberRepository.findAllByBandIdIn(bands).groupBy { it.band.id }
        val bandTiers = bands.map { bandId -> membersByBand[bandId].orEmpty().filter { it.member !in excluded } }
        val successor = SuccessorSelector.oldestFromHighestTier(bandTiers) { it.createdAt }?.member ?: return null
        selectionMemberRepository.save(TrackSelectionMember.create(selection, successor))
        return successor
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
