package com.bandage.v1.domain.band.service

import com.bandage.v1.domain.band.dto.req.BandApplicationPagingQuery
import com.bandage.v1.domain.band.dto.req.BandCreateRequest
import com.bandage.v1.domain.band.dto.req.BandPagingQuery
import com.bandage.v1.domain.band.dto.res.BandApplicationInfoResponse
import com.bandage.v1.domain.band.dto.res.BandInfoResponse
import com.bandage.v1.domain.band.dto.res.BandResponse
import com.bandage.v1.domain.band.model.Band
import com.bandage.v1.domain.band.model.BandApplication
import com.bandage.v1.domain.band.model.BandMember
import com.bandage.v1.domain.band.model.enums.ApplicationStatus
import com.bandage.v1.domain.band.model.enums.BandRole
import com.bandage.v1.domain.band.repository.BandApplicationRepository
import com.bandage.v1.domain.band.repository.BandMemberRepository
import com.bandage.v1.domain.band.repository.BandRepository
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

    fun getBandApplicationsByCursor(
        bandId: UUID,
        query: BandApplicationPagingQuery,
        memberId: Long,
    ): CursorResponse<BandApplicationInfoResponse, UUID> {
        val band = getBand(bandId)
        isMemberBandLeader(band, memberId)
        val result = applicationRepository.findAllByPaging(query.lastId, query.pageSize, query.status, band)
        return CursorResponse(
            content = result.content.map { BandApplicationInfoResponse.of(it) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    @Transactional
    fun createBandApplication(
        bandId: UUID,
        memberId: Long,
    ) {
        val band = getBand(bandId)
        isBandMemberAlreadyExists(band, memberId)
        isBandApplicationAlreadyExists(band, memberId)

        applicationRepository.save(
            BandApplication.create(
                band = band,
                member = memberId,
            ),
        )
    }

    @Transactional
    fun withdrawBandApplication(
        bandId: UUID,
        memberId: Long,
    ) {
        val band = getBand(bandId)
        isBandMemberAlreadyExists(band, memberId)
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
        isMemberBandLeader(band, memberId)

        val application = getPendingBandApplicationById(bandApplicationId)
        isApplicationBelongsToBand(band, application)

        when (status) {
            ApplicationStatus.APPROVED -> approve(band, application, memberId)
            ApplicationStatus.REJECTED -> application.updateStatus(ApplicationStatus.REJECTED)
            else -> throw BusinessException(ErrorCode.INVALID_INPUT_VALUE)
        }
    }

    // TODO: 알림 이벤트 publish 구현
    fun changeLeader(
        bandId: UUID,
        bandMemberId: UUID,
        memberId: Long,
    ) {
        val band = getBand(bandId)
        isMemberBandLeader(band, memberId)
        val currentLeader = getCurrentLeader(band, memberId)
        val newLeader = getBandMember(bandMemberId)
        newLeader.promoteToLeader()
        currentLeader.dismissFromLeader()
        confirmOnlyOneLeader(band)
    }

    // --- 내부 유틸리티 메서드 ---

    private fun getBand(bandId: UUID): Band =
        bandRepository.findByIdOrNull(bandId)
            ?: throw BusinessException(ErrorCode.BAND_NOT_FOUND)

    private fun getBandMember(bandMemberId: UUID): BandMember =
        bandMemberRepository.findByIdOrNull(bandMemberId)
            ?: throw BusinessException(ErrorCode.BAND_MEMBER_NOT_FOUND)

    private fun getCurrentLeader(
        band: Band,
        memberId: Long,
    ): BandMember =
        bandMemberRepository.findByBandAndMemberAndRole(band, memberId, BandRole.LEADER)
            ?: throw BusinessException(ErrorCode.BAND_MEMBER_NOT_FOUND)

    private fun isBandMemberAlreadyExists(
        band: Band,
        member: Long,
    ) {
        if (bandMemberRepository.existsBandMemberByBandAndMember(band, member)) {
            throw BusinessException(ErrorCode.BAND_MEMBER_ALREADY_EXISTS)
        }
    }

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

    private fun isBandApplicationAlreadyExists(
        band: Band,
        member: Long,
    ) {
        if (applicationRepository.existsByBandAndMemberAndStatus(band, member, ApplicationStatus.PENDING)) {
            throw BusinessException(ErrorCode.DUPLICATE_BAND_APPLICATION)
        }
    }

    private fun getPendingBandApplicationByMember(
        band: Band,
        member: Long,
    ): BandApplication =
        applicationRepository.findByBandAndMemberAndStatus(band, member, ApplicationStatus.PENDING)
            ?: throw BusinessException(ErrorCode.UNABLE_TO_WITHDRAW)

    private fun getPendingBandApplicationById(bandApplicationId: UUID): BandApplication =
        applicationRepository.findByIdAndStatus(bandApplicationId, ApplicationStatus.PENDING)
            ?: throw BusinessException(ErrorCode.BAND_APPLICATION_NOT_FOUND)

    private fun isApplicationBelongsToBand(
        band: Band,
        application: BandApplication,
    ) {
        if (application.band != band) {
            throw BusinessException(ErrorCode.BAND_APPLICATION_NOT_BELONGS_TO_BAND)
        }
    }

    private fun isMemberBandLeader(
        band: Band,
        memberId: Long,
    ) {
        if (!bandMemberRepository.existsByBandAndMemberAndRole(band, memberId, BandRole.LEADER)) {
            throw BusinessException(ErrorCode.NOT_A_LEADER)
        }
    }

    private fun confirmOnlyOneLeader(band: Band) {
        if (bandMemberRepository.countByBandAndRole(band, BandRole.LEADER) != 1) {
            throw BusinessException(ErrorCode.ABNORMAL_LEADER_COUNT)
        }
    }

    private fun approve(
        band: Band,
        application: BandApplication,
        leaderId: Long,
    ) {
        isBandMemberAlreadyExists(band, application.member)
        application.updateStatus(ApplicationStatus.APPROVED)
        application.markProcessedBy(leaderId)
        createBandMember(band, application.member, BandRole.MEMBER)
    }
}
