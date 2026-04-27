package com.bandage.v1.domain.band.service

import com.bandage.v1.domain.band.dto.req.BandApplicationPagingQuery
import com.bandage.v1.domain.band.dto.req.BandCreateRequest
import com.bandage.v1.domain.band.dto.req.BandMemberRoleUpdateRequest
import com.bandage.v1.domain.band.dto.req.BandPagingQuery
import com.bandage.v1.domain.band.dto.req.BandSearchQuery
import com.bandage.v1.domain.band.dto.req.BandUpdateRequest
import com.bandage.v1.domain.band.dto.res.BandApplicationInfoResponse
import com.bandage.v1.domain.band.dto.res.BandInfoResponse
import com.bandage.v1.domain.band.dto.res.BandMemberInfoResponse
import com.bandage.v1.domain.band.dto.res.BandResponse
import com.bandage.v1.domain.band.dto.res.MyBandInfoResponse
import com.bandage.v1.domain.band.model.Band
import com.bandage.v1.domain.band.model.BandApplication
import com.bandage.v1.domain.band.model.BandMember
import com.bandage.v1.domain.band.model.enums.ApplicationStatus
import com.bandage.v1.domain.band.model.enums.BandRole
import com.bandage.v1.domain.band.repository.BandApplicationRepository
import com.bandage.v1.domain.band.repository.BandMemberRepository
import com.bandage.v1.domain.band.repository.BandRepository
import com.bandage.v1.domain.member.repository.MemberRepository
import com.bandage.v1.global.common.response.CursorResponse
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
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
) {
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

    @Transactional
    fun createBandApplication(
        bandId: UUID,
        memberId: Long,
    ) {
        val band = getBand(bandId)
        validateBandMemberNotExists(band, memberId)
        validateBandApplicationNotExists(band, memberId)

        applicationRepository.save(
            BandApplication.create(
                band = band,
                member = memberId,
            ),
        )
    }

    fun getOnlyOneBand(bandId: UUID): BandInfoResponse =
        BandInfoResponse.of(
            getBand(bandId),
        )

    fun getBandsByCursor(query: BandPagingQuery): CursorResponse<BandInfoResponse, UUID> {
        val result = bandRepository.findAllByPaging(query.lastId, query.pageSize)
        return CursorResponse(
            content = result.content.map { BandInfoResponse.of(it) },
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
            content = result.content.map { MyBandInfoResponse.of(it.band, it.role) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    fun searchBandsByCursor(query: BandSearchQuery): CursorResponse<BandInfoResponse, UUID> {
        val result = bandRepository.searchByNameAndPaging(query.keyword, query.lastId, query.pageSize)
        return CursorResponse(
            content = result.content.map { BandInfoResponse.of(it) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    fun getOnlyOneBandMember(bandMemberId: UUID): BandMemberInfoResponse {
        val bm = getBandMemberById(bandMemberId)
        val member = memberRepository.findById(bm.member).orElse(null)
        return BandMemberInfoResponse.of(bm, member?.name, null)
    }

    fun getBandMembersByCursor(
        query: BandPagingQuery,
        bandId: UUID,
    ): CursorResponse<BandMemberInfoResponse, UUID> {
        val band = getBand(bandId)
        val result = bandMemberRepository.findAllByPaging(query.lastId, query.pageSize, band)
        val nameMap = memberNameMap(result.content.map { it.member })
        return CursorResponse(
            content = result.content.map { BandMemberInfoResponse.of(it, nameMap[it.member], null) },
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
        val nameMap = memberNameMap(result.content.map { it.member })
        return CursorResponse(
            content = result.content.map { BandApplicationInfoResponse.of(it, nameMap[it.member], null) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    private fun memberNameMap(memberIds: List<Long>): Map<Long, String> =
        if (memberIds.isEmpty()) {
            emptyMap()
        } else {
            memberRepository.findAllById(memberIds.distinct()).associate { it.id to it.name }
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

    // TODO: 알림 이벤트 publish 구현
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
