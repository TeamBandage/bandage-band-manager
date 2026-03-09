package com.bandage.v1.domain.band.repository

import com.bandage.v1.domain.band.model.Band
import com.bandage.v1.domain.band.model.BandApplication
import com.bandage.v1.domain.band.model.enums.ApplicationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BandApplicationRepository : JpaRepository<BandApplication, UUID> {
    fun existsByBandAndMemberAndStatus(
        band: Band,
        member: Long,
        status: ApplicationStatus,
    ): Boolean
}
