package com.bandage.v1.domain.band.repository

import com.bandage.v1.domain.band.model.Band
import com.bandage.v1.domain.band.model.BandMember
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BandMemberRepository : JpaRepository<BandMember, UUID> {
    fun existsBandMemberByBandAndMember(
        band: Band,
        member: Long,
    ): Boolean
}
