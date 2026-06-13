package com.bandage.bandmanager.domain.band.repository

import com.bandage.bandmanager.domain.band.model.Band
import com.bandage.bandmanager.domain.band.model.BandApplication
import com.bandage.bandmanager.domain.band.model.enums.ApplicationStatus
import org.springframework.data.jpa.repository.JpaRepository
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
}
