package com.bandage.bandmanager.domain.band.repository

import com.bandage.bandmanager.domain.band.model.Band
import com.bandage.bandmanager.domain.band.model.BandApplication
import com.bandage.bandmanager.domain.band.model.enums.ApplicationStatus
import com.bandage.bandmanager.global.common.response.CursorResponse
import java.util.UUID

interface BandApplicationRepositoryCustom {
    fun findAllByPaging(
        lastId: UUID?,
        pageSize: Int,
        status: ApplicationStatus,
        band: Band,
    ): CursorResponse<BandApplication, UUID>
}
