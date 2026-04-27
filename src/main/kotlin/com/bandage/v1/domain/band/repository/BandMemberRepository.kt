package com.bandage.v1.domain.band.repository

import com.bandage.v1.domain.band.model.Band
import com.bandage.v1.domain.band.model.BandMember
import com.bandage.v1.domain.band.model.enums.BandRole
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
}
