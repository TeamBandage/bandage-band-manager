package com.bandage.bandmanager.domain.band.repository

import com.bandage.bandmanager.domain.band.model.Band
import com.bandage.bandmanager.domain.band.model.BandMember
import com.bandage.bandmanager.domain.band.model.enums.BandRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BandMemberRepository :
    JpaRepository<BandMember, UUID>,
    BandMemberRepositoryCustom {
    @Query("SELECT bm.band.id FROM BandMember bm WHERE bm.member = :memberId")
    fun findAllBandIdsByMember(
        @Param("memberId") memberId: Long,
    ): List<UUID>

    @Query("SELECT bm.member FROM BandMember bm WHERE bm.band.id = :bandId")
    fun findAllMemberIdsByBand(
        @Param("bandId") bandId: UUID,
    ): List<Long>

    @Query("SELECT bm FROM BandMember bm JOIN FETCH bm.band WHERE bm.band.id IN :bandIds")
    fun findAllByBandIdIn(
        @Param("bandIds") bandIds: Collection<UUID>,
    ): List<BandMember>

    fun existsBandMemberByBandAndMember(
        band: Band,
        member: Long,
    ): Boolean

    fun existsByBandAndMemberAndRole(
        band: Band,
        member: Long,
        role: BandRole,
    ): Boolean

    fun findByBandAndMemberAndRole(
        band: Band,
        memberId: Long,
        role: BandRole,
    ): BandMember?

    fun findByBandAndMember(
        band: Band,
        memberId: Long,
    ): BandMember?

    fun findTopByBandAndMemberNotOrderByCreatedAtAsc(
        band: Band,
        leavingMemberId: Long,
    ): BandMember?

    fun countByBandAndRole(
        band: Band,
        role: BandRole,
    ): Int

    fun countByBand(band: Band): Int

    fun findAllByBand(band: Band): List<BandMember>

    fun countByMember(member: Long): Long

    // 알림(BAND_APPLICATION) 수신자 조회: 밴드의 특정 역할 멤버 목록
    @Query("SELECT bm FROM BandMember bm WHERE bm.band.id = :bandId AND bm.role = :role")
    fun findAllByBandIdAndRole(
        @Param("bandId") bandId: UUID,
        @Param("role") role: BandRole,
    ): List<BandMember>
}
