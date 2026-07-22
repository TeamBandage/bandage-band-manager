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
    /** 회원의 특정 밴드에 대한 최신 가입 신청 단건. (band, member) 당 isLatest=true 는 유일하다. */
    fun findByBandAndMemberAndIsLatestTrue(
        band: Band,
        member: Long,
    ): BandApplication?

    /** 회원의 특정 밴드에 대한 latest 신청 전체. 새 신청 생성 전 일괄 outdated 처리용(오염 데이터 방어). */
    fun findAllByBandAndMemberAndIsLatestTrue(
        band: Band,
        member: Long,
    ): List<BandApplication>

    /** 회원의 특정 상태 가입 신청을 밴드와 함께 일괄 조회(탈퇴 시 LEAVED 처리 배치용). */
    @Query("SELECT a FROM BandApplication a JOIN FETCH a.band WHERE a.member = :memberId AND a.status = :status")
    fun findAllByMemberAndStatusFetchBand(
        @Param("memberId") memberId: Long,
        @Param("status") status: ApplicationStatus,
    ): List<BandApplication>
}
