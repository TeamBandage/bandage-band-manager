package com.bandage.v1.domain.performance.service

import com.bandage.v1.domain.band.repository.BandMemberRepository
import com.bandage.v1.domain.band.repository.BandRepository
import com.bandage.v1.domain.member.repository.MemberRepository
import com.bandage.v1.domain.performance.dto.req.PerformanceCreateRequest
import com.bandage.v1.domain.performance.dto.req.PerformanceInvitationCreateRequest
import com.bandage.v1.domain.performance.dto.req.PerformancePagingQuery
import com.bandage.v1.domain.performance.dto.req.PerformanceSearchQuery
import com.bandage.v1.domain.performance.dto.req.PerformanceSetlistAddRequest
import com.bandage.v1.domain.performance.dto.req.PerformanceUpdateRequest
import com.bandage.v1.domain.performance.dto.res.PerformanceBandMemberSummary
import com.bandage.v1.domain.performance.dto.res.PerformanceBandSummary
import com.bandage.v1.domain.performance.dto.res.PerformanceDetailResponse
import com.bandage.v1.domain.performance.dto.res.PerformanceInvitationResponse
import com.bandage.v1.domain.performance.dto.res.PerformanceListResponse
import com.bandage.v1.domain.performance.dto.res.PerformanceResponse
import com.bandage.v1.domain.performance.dto.res.PerformanceSetlistResponse
import com.bandage.v1.domain.performance.dto.res.PerformanceSetlistSummary
import com.bandage.v1.domain.performance.model.Performance
import com.bandage.v1.domain.performance.model.PerformanceInvitation
import com.bandage.v1.domain.performance.model.PerformanceManager
import com.bandage.v1.domain.performance.model.PerformanceSetlist
import com.bandage.v1.domain.performance.model.enums.PerformanceInvitationStatus
import com.bandage.v1.domain.performance.repository.PerformanceInvitationRepository
import com.bandage.v1.domain.performance.repository.PerformanceManagerRepository
import com.bandage.v1.domain.performance.repository.PerformanceRepository
import com.bandage.v1.domain.performance.repository.PerformanceSetlistRepository
import com.bandage.v1.domain.setlist.model.Setlist
import com.bandage.v1.domain.setlist.repository.SetlistBandRepository
import com.bandage.v1.domain.setlist.repository.SetlistRepository
import com.bandage.v1.global.common.response.CursorResponse
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import com.bandage.v1.global.infra.s3.CloudFrontUrlResolver
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class PerformanceService(
    private val performanceRepository: PerformanceRepository,
    private val performanceSetlistRepository: PerformanceSetlistRepository,
    private val performanceManagerRepository: PerformanceManagerRepository,
    private val performanceInvitationRepository: PerformanceInvitationRepository,
    private val setlistRepository: SetlistRepository,
    private val setlistBandRepository: SetlistBandRepository,
    private val bandRepository: BandRepository,
    private val bandMemberRepository: BandMemberRepository,
    private val memberRepository: MemberRepository,
    private val cloudFrontUrlResolver: CloudFrontUrlResolver,
) {
    @Transactional
    fun createPerformance(
        request: PerformanceCreateRequest,
        memberId: Long,
    ): PerformanceResponse {
        val performance =
            performanceRepository.save(
                Performance.create(
                    title = request.title,
                    startAt = request.startAt,
                    durationMinutes = request.durationMinutes,
                    venue = request.venue,
                ),
            )
        (request.setlistIds ?: emptyList()).distinct().forEach { setlistId ->
            requireSetlist(setlistId)
            validateSetlistAccessible(setlistId, memberId)
            performanceSetlistRepository.save(PerformanceSetlist.create(performance = performance, setlistId = setlistId))
        }
        performanceManagerRepository.save(PerformanceManager.createOwner(performance = performance, member = memberId))
        return PerformanceResponse.of(performance)
    }

    fun getPerformances(query: PerformancePagingQuery): CursorResponse<PerformanceListResponse, UUID> {
        val result = performanceRepository.findAllByPaging(query.lastId, query.pageSize)
        val summaries = buildSetlistSummaries(result.content.flatMap { p -> p.setlists.map { it.setlistId } })
        return CursorResponse(
            content = result.content.map { PerformanceListResponse.of(it, summaries) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    fun getPerformancesByBand(
        bandId: UUID,
        query: PerformancePagingQuery,
    ): CursorResponse<PerformanceListResponse, UUID> {
        val result = performanceRepository.findAllByBandIdAndPaging(bandId, query.lastId, query.pageSize)
        val summaries = buildSetlistSummaries(result.content.flatMap { p -> p.setlists.map { it.setlistId } })
        return CursorResponse(
            content = result.content.map { PerformanceListResponse.of(it, summaries) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    fun getMyPerformancesByCursor(
        memberId: Long,
        query: PerformancePagingQuery,
    ): CursorResponse<PerformanceListResponse, UUID> {
        val bandIds = bandMemberRepository.findAllBandIdsByMember(memberId)
        if (bandIds.isEmpty()) {
            return CursorResponse(content = emptyList(), nextCursor = null, hasNext = false)
        }
        val result = performanceRepository.findAllByBandIdsAndPaging(bandIds, query.lastId, query.pageSize)
        val summaries = buildSetlistSummaries(result.content.flatMap { p -> p.setlists.map { it.setlistId } })
        return CursorResponse(
            content = result.content.map { PerformanceListResponse.of(it, summaries) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    fun searchPerformancesByCursor(query: PerformanceSearchQuery): CursorResponse<PerformanceListResponse, UUID> {
        val result = performanceRepository.searchByTitleAndPaging(query.keyword, query.lastId, query.pageSize)
        val summaries = buildSetlistSummaries(result.content.flatMap { p -> p.setlists.map { it.setlistId } })
        return CursorResponse(
            content = result.content.map { PerformanceListResponse.of(it, summaries) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    fun getPerformanceDetail(performanceId: UUID): PerformanceDetailResponse {
        val performance = getPerformance(performanceId)
        val summaries = buildSetlistSummaries(performance.setlists.map { it.setlistId })
        return PerformanceDetailResponse.of(performance, summaries)
    }

    @Transactional
    fun updatePerformance(
        performanceId: UUID,
        request: PerformanceUpdateRequest,
        memberId: Long,
    ) {
        val performance = getPerformance(performanceId)
        validateParticipant(performance, memberId)
        request.title?.let { performance.updateTitle(it) }
        if (request.startAt != null || request.durationMinutes != null) {
            performance.updateTimeInfo(
                startAt = request.startAt ?: performance.timeInfo.startAt,
                durationMinutes = request.durationMinutes ?: performance.timeInfo.durationMinutes,
            )
        }
        request.venue?.let { performance.updateVenue(it) }
    }

    @Transactional
    fun addSetlists(
        performanceId: UUID,
        request: PerformanceSetlistAddRequest,
        memberId: Long,
    ): List<PerformanceSetlistResponse> {
        val performance = getPerformance(performanceId)
        validateParticipant(performance, memberId)
        return request.setlistIds.distinct().mapNotNull { setlistId ->
            if (performanceSetlistRepository.existsByPerformanceAndSetlistId(performance, setlistId)) return@mapNotNull null
            requireSetlist(setlistId)
            validateSetlistAccessible(setlistId, memberId)
            val saved = performanceSetlistRepository.save(PerformanceSetlist.create(performance = performance, setlistId = setlistId))
            PerformanceSetlistResponse.of(saved)
        }
    }

    @Transactional
    fun removeSetlist(
        performanceId: UUID,
        setlistId: UUID,
        memberId: Long,
    ) {
        val performance = getPerformance(performanceId)
        val participant = requireParticipant(performance, memberId)
        val ps =
            performanceSetlistRepository.findByPerformanceAndSetlistId(performance, setlistId)
                ?: throw BusinessException(ErrorCode.PERFORMANCE_SETLIST_NOT_FOUND)
        // OWNER 는 공연 내 모든 셋리스트를 제거할 수 있고, MANAGER 는 본인이 접근 가능한 셋리스트만 제거할 수 있다.
        if (!participant.isOwner()) {
            validateSetlistAccessible(setlistId, memberId)
        }
        performanceSetlistRepository.delete(ps)
    }

    @Transactional
    fun deletePerformance(
        performanceId: UUID,
        memberId: Long,
    ) {
        val performance = getPerformance(performanceId)
        validateOwner(performance, memberId)
        performance.markAsDeleted(memberId)
    }

    @Transactional
    fun sendInvitation(
        performanceId: UUID,
        request: PerformanceInvitationCreateRequest,
        ownerId: Long,
    ): PerformanceInvitationResponse {
        val performance = getPerformance(performanceId)
        validateOwner(performance, ownerId)
        val invitedMemberId = request.memberId
        if (!memberRepository.existsById(invitedMemberId)) {
            throw BusinessException(ErrorCode.MEMBER_NOT_FOUND)
        }
        if (performanceManagerRepository.existsByPerformanceAndMember(performance, invitedMemberId)) {
            throw BusinessException(ErrorCode.ALREADY_PERFORMANCE_MANAGER)
        }
        if (performanceInvitationRepository.existsByPerformanceAndInvitedMemberAndStatus(
                performance,
                invitedMemberId,
                PerformanceInvitationStatus.PENDING,
            )
        ) {
            throw BusinessException(ErrorCode.PERFORMANCE_INVITATION_ALREADY_EXISTS)
        }
        val saved =
            performanceInvitationRepository.save(
                PerformanceInvitation.create(
                    performance = performance,
                    invitedMember = invitedMemberId,
                    invitedBy = ownerId,
                ),
            )
        return toInvitationResponses(listOf(saved)).first()
    }

    fun getInvitations(
        performanceId: UUID,
        ownerId: Long,
    ): List<PerformanceInvitationResponse> {
        val performance = getPerformance(performanceId)
        validateOwner(performance, ownerId)
        return toInvitationResponses(performanceInvitationRepository.findAllByPerformanceOrderByCreatedAtDesc(performance))
    }

    fun getMyInvitations(memberId: Long): List<PerformanceInvitationResponse> =
        toInvitationResponses(
            performanceInvitationRepository.findAllByInvitedMemberAndStatusOrderByCreatedAtDesc(
                memberId,
                PerformanceInvitationStatus.PENDING,
            ),
        )

    @Transactional
    fun respondInvitation(
        performanceId: UUID,
        invitationId: UUID,
        memberId: Long,
        status: PerformanceInvitationStatus,
    ) {
        val performance = getPerformance(performanceId)
        val invitation =
            performanceInvitationRepository.findByIdAndStatus(invitationId, PerformanceInvitationStatus.PENDING)
                ?: throw BusinessException(ErrorCode.PERFORMANCE_INVITATION_NOT_FOUND)
        if (invitation.performance.id != performance.id) {
            throw BusinessException(ErrorCode.PERFORMANCE_INVITATION_NOT_FOUND)
        }
        if (invitation.invitedMember != memberId) {
            throw BusinessException(ErrorCode.PERFORMANCE_INVITATION_FORBIDDEN)
        }
        when (status) {
            PerformanceInvitationStatus.ACCEPTED -> {
                invitation.updateStatus(PerformanceInvitationStatus.ACCEPTED)
                invitation.markProcessedBy(memberId)
                if (!performanceManagerRepository.existsByPerformanceAndMember(performance, memberId)) {
                    performanceManagerRepository.save(PerformanceManager.createManager(performance = performance, member = memberId))
                }
            }
            PerformanceInvitationStatus.REJECTED -> {
                invitation.updateStatus(PerformanceInvitationStatus.REJECTED)
                invitation.markProcessedBy(memberId)
            }
            else -> throw BusinessException(ErrorCode.INVALID_INPUT_VALUE)
        }
    }

    private fun buildSetlistSummaries(setlistIds: Collection<UUID>): Map<UUID, PerformanceSetlistSummary> {
        if (setlistIds.isEmpty()) return emptyMap()
        val distinctSetlistIds = setlistIds.toSet()
        val setlists = setlistRepository.findAllById(distinctSetlistIds).associateBy { it.id }
        val setlistBands = setlistBandRepository.findAllBySetlistIdIn(distinctSetlistIds)
        val bandIdsBySetlistId: Map<UUID, List<UUID>> =
            setlistBands.groupBy({ it.setlistId }, { it.bandId })
        val bandSummariesByBandId = buildBandSummaries(setlistBands.map { it.bandId })
        return distinctSetlistIds
            .mapNotNull { setlistId ->
                val setlist = setlists[setlistId] ?: return@mapNotNull null
                setlistId to
                    PerformanceSetlistSummary(
                        setlistId = setlist.id,
                        title = setlist.title,
                        bands = (bandIdsBySetlistId[setlistId] ?: emptyList()).mapNotNull { bandSummariesByBandId[it] },
                    )
            }.toMap()
    }

    private fun buildBandSummaries(bandIds: Collection<UUID>): Map<UUID, PerformanceBandSummary> {
        if (bandIds.isEmpty()) return emptyMap()
        val distinctBandIds = bandIds.toSet()
        val bands = bandRepository.findAllById(distinctBandIds)
        val bandMembers = bandMemberRepository.findAllByBandIdIn(distinctBandIds)
        val members = memberRepository.findAllById(bandMembers.map { it.member }.toSet()).associateBy { it.id }
        val membersByBandId = bandMembers.groupBy { it.band.id }
        return bands.associate { band ->
            band.id to
                PerformanceBandSummary(
                    bandId = band.id,
                    bandName = band.name,
                    members =
                        membersByBandId[band.id].orEmpty().mapNotNull { bm ->
                            members[bm.member]?.let {
                                PerformanceBandMemberSummary(
                                    userId = it.id,
                                    name = it.name,
                                    profileImg = cloudFrontUrlResolver.resolveOrNull(it.profileImg),
                                    role = bm.role,
                                )
                            }
                        },
                )
        }
    }

    private fun toInvitationResponses(invitations: List<PerformanceInvitation>): List<PerformanceInvitationResponse> {
        if (invitations.isEmpty()) return emptyList()
        val members = memberRepository.findAllById(invitations.map { it.invitedMember }.toSet()).associateBy { it.id }
        return invitations.map { invitation ->
            val member = members[invitation.invitedMember]
            PerformanceInvitationResponse.of(
                invitation = invitation,
                invitedMemberName = member?.name,
                invitedMemberProfileImg = cloudFrontUrlResolver.resolveOrNull(member?.profileImg),
            )
        }
    }

    fun getPerformance(performanceId: UUID): Performance =
        performanceRepository.findByIdOrNull(performanceId)
            ?: throw BusinessException(ErrorCode.PERFORMANCE_NOT_FOUND)

    private fun requireSetlist(setlistId: UUID): Setlist =
        setlistRepository.findByIdOrNull(setlistId)
            ?: throw BusinessException(ErrorCode.SETLIST_NOT_FOUND)

    private fun validateSetlistAccessible(
        setlistId: UUID,
        memberId: Long,
    ) {
        if (!setlistRepository.isAccessibleMember(setlistId, memberId)) {
            throw BusinessException(ErrorCode.SETLIST_FORBIDDEN)
        }
    }

    private fun requireParticipant(
        performance: Performance,
        memberId: Long,
    ): PerformanceManager =
        performanceManagerRepository.findByPerformanceAndMember(performance, memberId)
            ?: throw BusinessException(ErrorCode.NOT_A_PERFORMANCE_MANAGER)

    private fun validateParticipant(
        performance: Performance,
        memberId: Long,
    ) {
        requireParticipant(performance, memberId)
    }

    private fun validateOwner(
        performance: Performance,
        memberId: Long,
    ) {
        if (!requireParticipant(performance, memberId).isOwner()) {
            throw BusinessException(ErrorCode.NOT_A_PERFORMANCE_OWNER)
        }
    }
}
