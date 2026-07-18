package com.bandage.bandmanager.domain.band.service

import com.bandage.bandmanager.domain.band.dto.req.BandApplicationPagingQuery
import com.bandage.bandmanager.domain.band.dto.req.BandCreateRequest
import com.bandage.bandmanager.domain.band.dto.req.BandMemberRoleUpdateRequest
import com.bandage.bandmanager.domain.band.dto.req.BandPagingQuery
import com.bandage.bandmanager.domain.band.dto.req.BandSearchQuery
import com.bandage.bandmanager.domain.band.dto.req.BandUpdateRequest
import com.bandage.bandmanager.domain.band.dto.req.MyBandApplicationPagingQuery
import com.bandage.bandmanager.domain.band.dto.res.BandApplicationInfoResponse
import com.bandage.bandmanager.domain.band.dto.res.BandInfoResponse
import com.bandage.bandmanager.domain.band.dto.res.BandMemberInfoResponse
import com.bandage.bandmanager.domain.band.dto.res.BandResponse
import com.bandage.bandmanager.domain.band.dto.res.MyBandApplicationInfoResponse
import com.bandage.bandmanager.domain.band.dto.res.MyBandInfoResponse
import com.bandage.bandmanager.domain.band.model.Band
import com.bandage.bandmanager.domain.band.model.BandApplication
import com.bandage.bandmanager.domain.band.model.BandMember
import com.bandage.bandmanager.domain.band.model.enums.ApplicationStatus
import com.bandage.bandmanager.domain.band.model.enums.BandRole
import com.bandage.bandmanager.domain.band.repository.BandApplicationRepository
import com.bandage.bandmanager.domain.band.repository.BandMemberRepository
import com.bandage.bandmanager.domain.band.repository.BandRepository
import com.bandage.bandmanager.domain.member.repository.MemberRepository
import com.bandage.bandmanager.global.authority.MemberAuthorityCleanupHandler
import com.bandage.bandmanager.global.authority.ResourceAuthorityType
import com.bandage.bandmanager.global.authority.SuccessorSelector
import com.bandage.bandmanager.global.common.response.CursorResponse
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import com.bandage.bandmanager.global.infra.s3.CloudFrontUrlResolver
import com.bandage.bandmanager.global.infra.s3.ImagePresignRequest
import com.bandage.bandmanager.global.infra.s3.ImagePresignResponse
import com.bandage.bandmanager.global.infra.s3.ImagePresignSupport
import com.bandage.bandmanager.global.notify.annotation.Notify
import com.bandage.bandmanager.global.notify.annotation.NotifyCategory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class BandService(
    private val bandRepository: BandRepository,
    private val applicationRepository: BandApplicationRepository,
    private val bandMemberRepository: BandMemberRepository,
    private val memberRepository: MemberRepository,
    private val cloudFrontUrlResolver: CloudFrontUrlResolver,
    private val imagePresignSupport: ImagePresignSupport,
) : MemberAuthorityCleanupHandler {
    override val authorityType: ResourceAuthorityType = ResourceAuthorityType.BAND_LEADERSHIP

    // TODO: profileImg multi-part 처리 구현
    @Transactional
    fun createBand(
        request: BandCreateRequest,
        memberId: Long,
    ): BandResponse {
        if (bandRepository.existsByName(request.name)) {
            throw BusinessException(ErrorCode.DUPLICATE_BAND_NAME)
        }
        val band =
            bandRepository.save(
                Band.create(
                    name = request.name,
                    description = request.description,
                    profileImg = request.profileImg,
                ),
            )
        createBandMember(band, memberId, BandRole.LEADER)
        return BandResponse.of(band)
    }

    @Notify(NotifyCategory.BAND_APPLICATION)
    @Transactional
    fun createBandApplication(
        bandId: UUID,
        memberId: Long,
    ) {
        val band = getBand(bandId)
        validateBandMemberNotExists(band, memberId)
        validateBandApplicationNotExists(band, memberId)

        applicationRepository.findByBandAndMemberAndIsLatestTrue(band, memberId)?.let {
            it.markAsOutdated()
            // 새 신청 INSERT 전에 이전 건의 is_latest=false 를 먼저 반영해야 partial unique index
            // (band_id, member_id) WHERE is_latest 위반을 피한다. order_inserts 로 INSERT 가 UPDATE 보다
            // 먼저 flush 되므로 명시적 flush 로 순서를 보장한다.
            applicationRepository.flush()
        }

        applicationRepository.save(
            BandApplication.create(
                band = band,
                member = memberId,
            ),
        )
    }

    fun getOnlyOneBand(bandId: UUID): BandInfoResponse {
        val band = getBand(bandId)
        return BandInfoResponse.of(band, profileImageUrl(band.profileImg))
    }

    fun getBandsByCursor(query: BandPagingQuery): CursorResponse<BandInfoResponse, UUID> {
        val result = bandRepository.findAllByPaging(query.lastId, query.pageSize)
        return CursorResponse(
            content = result.content.map { BandInfoResponse.of(it, profileImageUrl(it.profileImg)) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    fun getMyBandsByCursor(
        memberId: Long,
        query: BandPagingQuery,
    ): CursorResponse<MyBandInfoResponse, UUID> {
        val result = bandRepository.findAllByMemberWithRoleAndPaging(memberId, query.lastId, query.pageSize)
        return CursorResponse(
            content = result.content.map { MyBandInfoResponse.of(it.band, it.role, profileImageUrl(it.band.profileImg)) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    fun searchBandsByCursor(query: BandSearchQuery): CursorResponse<BandInfoResponse, UUID> {
        val result = bandRepository.searchByNameAndPaging(query.keyword, query.lastId, query.pageSize)
        return CursorResponse(
            content = result.content.map { BandInfoResponse.of(it, profileImageUrl(it.profileImg)) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    private fun profileImageUrl(key: String?): String? = cloudFrontUrlResolver.resolveOrNull(key)

    fun getOnlyOneBandMember(bandMemberId: UUID): BandMemberInfoResponse {
        val bm = getBandMemberById(bandMemberId)
        val member = memberRepository.findById(bm.member).orElse(null)
        return BandMemberInfoResponse.of(bm, member?.name, profileImageUrl(member?.profileImg))
    }

    fun getBandMembersByCursor(
        query: BandPagingQuery,
        bandId: UUID,
    ): CursorResponse<BandMemberInfoResponse, UUID> {
        val band = getBand(bandId)
        val result = bandMemberRepository.findAllByPaging(query.lastId, query.pageSize, band)
        val profileMap = memberProfileMap(result.content.map { it.member })
        return CursorResponse(
            content =
                result.content.map {
                    val info = profileMap[it.member]
                    BandMemberInfoResponse.of(it, info?.name, profileImageUrl(info?.profileImg))
                },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    fun getBandApplicationsByCursor(
        bandId: UUID,
        query: BandApplicationPagingQuery,
        memberId: Long,
    ): CursorResponse<BandApplicationInfoResponse, UUID> {
        val band = getBand(bandId)
        validateMemberIsBandLeader(band, memberId)
        val result = applicationRepository.findAllByPaging(query.lastId, query.pageSize, query.status, band)
        val profileMap = memberProfileMap(result.content.map { it.member })
        return CursorResponse(
            content =
                result.content.map {
                    val info = profileMap[it.member]
                    BandApplicationInfoResponse.of(it, info?.name, profileImageUrl(info?.profileImg))
                },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    fun getMyApplicationsByCursor(
        memberId: Long,
        query: MyBandApplicationPagingQuery,
    ): CursorResponse<MyBandApplicationInfoResponse, UUID> {
        val result = applicationRepository.findAllByMemberPaging(query.lastId, query.pageSize, query.status, memberId)
        return CursorResponse(
            content = result.content.map { MyBandApplicationInfoResponse.of(it, profileImageUrl(it.band.profileImg)) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    fun getMyApplicationForBand(
        bandId: UUID,
        memberId: Long,
    ): MyBandApplicationInfoResponse {
        val band = getBand(bandId)
        val application =
            applicationRepository.findByBandAndMemberAndIsLatestTrue(band, memberId)
                ?: throw BusinessException(ErrorCode.BAND_APPLICATION_NOT_FOUND)
        return MyBandApplicationInfoResponse.of(application, profileImageUrl(band.profileImg))
    }

    private data class MemberProfileInfo(
        val name: String,
        val profileImg: String?,
    )

    private fun memberProfileMap(memberIds: List<Long>): Map<Long, MemberProfileInfo> =
        if (memberIds.isEmpty()) {
            emptyMap()
        } else {
            memberRepository
                .findAllById(memberIds.distinct())
                .associate { it.id to MemberProfileInfo(it.name, it.profileImg) }
        }

    @Transactional
    fun withdrawBandApplication(
        bandId: UUID,
        memberId: Long,
    ) {
        val band = getBand(bandId)
        val application = getPendingBandApplicationByMember(band, memberId)
        application.updateStatus(ApplicationStatus.WITHDRAWN)
    }

    @Notify(NotifyCategory.BAND_APPLICATION_RESULT)
    @Transactional
    fun processBandApplication(
        bandId: UUID,
        bandApplicationId: UUID,
        memberId: Long,
        status: ApplicationStatus,
    ) {
        val band = getBand(bandId)
        validateMemberIsBandLeader(band, memberId)

        val application = getPendingBandApplicationById(bandApplicationId)
        validateApplicationBelongsToBand(band, application)

        when (status) {
            ApplicationStatus.APPROVED -> approve(band, application, memberId)
            ApplicationStatus.REJECTED -> application.updateStatus(ApplicationStatus.REJECTED)
            else -> throw BusinessException(ErrorCode.INVALID_INPUT_VALUE)
        }
    }

    @Notify(NotifyCategory.AUTHORITY_PROMOTION)
    @Transactional
    fun changeLeader(
        bandId: UUID,
        bandMemberId: UUID,
        memberId: Long,
    ) {
        val band = getBand(bandId)
        validateMemberIsBandLeader(band, memberId)
        val currentLeader = getCurrentLeader(band, memberId)
        val newLeader = getBandMemberById(bandMemberId)
        switchLeader(
            from = currentLeader,
            to = newLeader,
        )
        validateOnlyOneLeader(band)
    }

    @Transactional
    fun updateBand(
        bandId: UUID,
        request: BandUpdateRequest,
        memberId: Long,
    ): BandResponse {
        val band = getBand(bandId)
        validateMemberIsBandLeader(band, memberId)
        var changed = false
        request.name
            ?.takeIf { it.isNotBlank() && it != band.name }
            ?.let {
                if (bandRepository.existsByName(it)) throw BusinessException(ErrorCode.DUPLICATE_BAND_NAME)
                band.updateName(it)
                changed = true
            }
        request.description
            ?.takeIf { it != band.description }
            ?.let {
                band.updateDescription(it)
                changed = true
            }
        request.profileImg
            ?.takeIf { it != band.profileImg }
            ?.let {
                band.updateImg(it)
                changed = true
            }
        if (!changed) throw BusinessException(ErrorCode.NO_CHANGE)
        return BandResponse.of(band)
    }

    @Transactional
    fun deleteProfileImage(
        bandId: UUID,
        memberId: Long,
    ) {
        val band = getBand(bandId)
        validateMemberIsBandLeader(band, memberId)
        band.deleteImg()
    }

    /** 밴드 프로필 이미지 업로드용 presigned URL 발급. 리더만 가능. 응답 objectKey 를 밴드 수정 시 profileImg 로 전달. */
    fun issueProfileImagePresignedUrl(
        bandId: UUID,
        request: ImagePresignRequest,
        memberId: Long,
    ): ImagePresignResponse {
        val band = getBand(bandId)
        validateMemberIsBandLeader(band, memberId)
        return imagePresignSupport.issue(request, "profile/band/$bandId")
    }

    @Transactional
    fun deleteBand(
        bandId: UUID,
        memberId: Long,
    ) {
        val band = getBand(bandId)
        validateMemberIsBandLeader(band, memberId)
        bandMemberRepository.findAllByBand(band).forEach { it.markAsDeleted(memberId) }
        band.markAsDeleted(memberId)
    }

    @Transactional
    fun kickMember(
        bandId: UUID,
        bandMemberId: UUID,
        leaderMemberId: Long,
    ) {
        val band = getBand(bandId)
        validateMemberIsBandLeader(band, leaderMemberId)
        val target = getBandMemberById(bandMemberId)
        if (target.band.id != band.id) throw BusinessException(ErrorCode.BAND_MEMBER_NOT_FOUND)
        if (target.role == BandRole.LEADER) throw BusinessException(ErrorCode.LEADER_CANNOT_LEAVE)
        target.markAsDeleted(leaderMemberId)
        band.decreaseMemberCnt()
    }

    @Transactional
    fun changeMemberRole(
        bandId: UUID,
        bandMemberId: UUID,
        leaderMemberId: Long,
        request: BandMemberRoleUpdateRequest,
    ) {
        val band = getBand(bandId)
        validateMemberIsBandLeader(band, leaderMemberId)
        val target = getBandMemberById(bandMemberId)
        if (target.band.id != band.id) throw BusinessException(ErrorCode.BAND_MEMBER_NOT_FOUND)
        if (target.role == request.role) throw BusinessException(ErrorCode.NO_CHANGE)
        target.changeRole(request.role)
    }

    @Transactional
    fun leaveBand(
        bandId: UUID,
        memberId: Long,
    ) {
        val band = getBand(bandId)
        val bandMember = getBandMemberByMember(band, memberId)
        val totalMemberCount = bandMemberRepository.countByBand(band)

        if (totalMemberCount <= 1) {
            processBandMemberStatusAsLeaved(bandMember, band, memberId)
            band.markAsDeleted(memberId)
            return
        }
        if (bandMember.role == BandRole.LEADER) {
            val nextLeader = getOldestBandMemberExcluding(band, memberId)
            switchLeader(from = bandMember, to = nextLeader)
        }

        processBandMemberStatusAsLeaved(bandMember, band, memberId)
        validateOnlyOneLeader(band)
    }

    /**
     * 회원 탈퇴 시 호출. 회원이 속한 모든 밴드를 정리한다(leaveBand 와 동일 정책).
     * - 마지막 멤버였던 밴드: 밴드 소프트 삭제
     * - 리더였던 밴드: ADMIN > MEMBER 순 최고참에게 리더 자동 양도 후 탈퇴 처리
     * - 그 외 밴드: 탈퇴 처리
     * 모든 밴드/멤버를 fetch join 으로 일괄 조회해 밴드별 추가 쿼리를 피한다.
     */
    @Transactional
    override fun cleanupOnWithdrawal(memberId: Long) {
        val bandIds = bandMemberRepository.findAllBandIdsByMember(memberId)
        if (bandIds.isEmpty()) return

        val membersByBand = bandMemberRepository.findAllByBandIdIn(bandIds).groupBy { it.band.id }
        val approvedApplicationByBand =
            applicationRepository
                .findAllByMemberAndStatusFetchBand(memberId, ApplicationStatus.APPROVED)
                .associateBy { it.band.id }

        bandIds.forEach { bandId ->
            val members = membersByBand[bandId] ?: return@forEach
            val leaving = members.firstOrNull { it.member == memberId } ?: return@forEach
            val band = leaving.band
            val remaining = members.filter { it.member != memberId }

            if (remaining.isEmpty()) {
                markBandMemberLeaved(leaving, approvedApplicationByBand[bandId], memberId)
                band.markAsDeleted(memberId)
                return@forEach
            }

            if (leaving.role == BandRole.LEADER) {
                switchLeader(from = leaving, to = selectBandSuccessor(remaining))
            }
            markBandMemberLeaved(leaving, approvedApplicationByBand[bandId], memberId)
        }
    }

    /** 사전 조회한 APPROVED 신청을 LEAVED 로 전환하고 소속을 소프트 삭제(밴드별 추가 쿼리 없음). */
    private fun markBandMemberLeaved(
        bandMember: BandMember,
        approvedApplication: BandApplication?,
        deleterId: Long,
    ) {
        approvedApplication?.updateStatus(ApplicationStatus.LEAVED)
        bandMember.markAsDeleted(deleterId)
    }

    private fun selectBandSuccessor(candidates: List<BandMember>): BandMember =
        SuccessorSelector.oldestFromHighestTier(
            tiers =
                listOf(
                    candidates.filter { it.role == BandRole.ADMIN },
                    candidates.filter { it.role == BandRole.MEMBER },
                ),
            createdAt = { it.createdAt },
        ) ?: candidates.minByOrNull { it.createdAt }!!

    // --- 내부 유틸리티 메서드 ---
    private fun createBandMember(
        band: Band,
        memberId: Long,
        role: BandRole,
    ) {
        bandMemberRepository.save(
            BandMember.create(
                band = band,
                member = memberId,
                role = role,
            ),
        )
    }

    private fun getBand(bandId: UUID): Band =
        bandRepository.findByIdOrNull(bandId)
            ?: throw BusinessException(ErrorCode.BAND_NOT_FOUND)

    private fun getPendingBandApplicationById(bandApplicationId: UUID): BandApplication =
        applicationRepository.findByIdAndStatus(bandApplicationId, ApplicationStatus.PENDING)
            ?: throw BusinessException(ErrorCode.BAND_APPLICATION_NOT_FOUND)

    private fun getPendingBandApplicationByMember(
        band: Band,
        member: Long,
    ): BandApplication =
        applicationRepository.findByBandAndMemberAndStatus(band, member, ApplicationStatus.PENDING)
            ?: throw BusinessException(ErrorCode.UNABLE_TO_WITHDRAW)

    private fun getBandMemberById(bandMemberId: UUID): BandMember =
        bandMemberRepository.findByIdOrNull(bandMemberId)
            ?: throw BusinessException(ErrorCode.BAND_MEMBER_NOT_FOUND)

    private fun getBandMemberByMember(
        band: Band,
        memberId: Long,
    ): BandMember =
        bandMemberRepository.findByBandAndMember(band, memberId)
            ?: throw BusinessException(ErrorCode.BAND_MEMBER_NOT_FOUND)

    private fun getCurrentLeader(
        band: Band,
        memberId: Long,
    ): BandMember =
        bandMemberRepository.findByBandAndMemberAndRole(band, memberId, BandRole.LEADER)
            ?: throw BusinessException(ErrorCode.BAND_MEMBER_NOT_FOUND)

    private fun getOldestBandMemberExcluding(
        band: Band,
        leavingMemberId: Long,
    ): BandMember =
        bandMemberRepository.findTopByBandAndMemberNotOrderByCreatedAtAsc(band, leavingMemberId)
            ?: throw BusinessException(ErrorCode.BAND_MEMBER_NOT_FOUND)

    private fun validateBandMemberNotExists(
        band: Band,
        member: Long,
    ) {
        if (bandMemberRepository.existsBandMemberByBandAndMember(band, member)) {
            throw BusinessException(ErrorCode.BAND_MEMBER_ALREADY_EXISTS)
        }
    }

    private fun validateBandApplicationNotExists(
        band: Band,
        member: Long,
    ) {
        if (applicationRepository.existsByBandAndMemberAndStatus(band, member, ApplicationStatus.PENDING)) {
            throw BusinessException(ErrorCode.DUPLICATE_BAND_APPLICATION)
        }
    }

    private fun validateApplicationBelongsToBand(
        band: Band,
        application: BandApplication,
    ) {
        if (application.band != band) {
            throw BusinessException(ErrorCode.BAND_APPLICATION_NOT_BELONGS_TO_BAND)
        }
    }

    private fun validateMemberIsBandLeader(
        band: Band,
        memberId: Long,
    ) {
        if (!bandMemberRepository.existsByBandAndMemberAndRole(band, memberId, BandRole.LEADER)) {
            throw BusinessException(ErrorCode.NOT_A_LEADER)
        }
    }

    private fun validateOnlyOneLeader(band: Band) {
        if (bandMemberRepository.countByBandAndRole(band, BandRole.LEADER) != 1) {
            throw BusinessException(ErrorCode.ABNORMAL_LEADER_COUNT)
        }
    }

    private fun approve(
        band: Band,
        application: BandApplication,
        leaderId: Long,
    ) {
        validateBandMemberNotExists(band, application.member)
        application.updateStatus(ApplicationStatus.APPROVED)
        application.markProcessedBy(leaderId)
        createBandMember(band, application.member, BandRole.MEMBER)
    }

    private fun switchLeader(
        from: BandMember,
        to: BandMember,
    ) {
        from.dismissFromLeader()
        to.promoteToLeader()
    }

    private fun processBandMemberStatusAsLeaved(
        bandMember: BandMember,
        band: Band,
        memberId: Long,
    ) {
        val application = applicationRepository.findByBandAndMemberAndStatus(band, memberId, ApplicationStatus.APPROVED)
        application?.updateStatus(ApplicationStatus.LEAVED)
        bandMember.markAsDeleted(memberId)
    }
}
