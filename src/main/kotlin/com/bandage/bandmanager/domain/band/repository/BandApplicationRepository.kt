package com.bandage.bandmanager.domain.band.repository

import com.bandage.bandmanager.domain.band.model.Band
import com.bandage.bandmanager.domain.band.model.BandApplication
import com.bandage.bandmanager.domain.band.model.enums.ApplicationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BandApplicationRepository :
    JpaRepository<BandApplication, UUID>,
    BandApplicationRepositoryCustom {
    fun existsByBandAndMemberAndStatus(
        band: Band,
        member: Long,
        status: ApplicationStatus,
    ): Boolean

    fun findByBandAndMemberAndStatus(
        band: Band,
        member: Long,
        status: ApplicationStatus,
    ): BandApplication?

    fun findByIdAndStatus(
        id: UUID,
        status: ApplicationStatus,
    ): BandApplication?

    /** 회원의 특정 밴드에 대한 가장 최근 가입 신청 단건(상태 무관). */
    fun findTopByBandAndMemberOrderByCreatedAtDesc(
        band: Band,
        member: Long,
    ): BandApplication?

    /** 회원의 특정 상태 가입 신청을 밴드와 함께 일괄 조회(탈퇴 시 LEAVED 처리 배치용). */
    @Query("SELECT a FROM BandApplication a JOIN FETCH a.band WHERE a.member = :memberId AND a.status = :status")
    fun findAllByMemberAndStatusFetchBand(
        @Param("memberId") memberId: Long,
        @Param("status") status: ApplicationStatus,
    ): List<BandApplication>
}
