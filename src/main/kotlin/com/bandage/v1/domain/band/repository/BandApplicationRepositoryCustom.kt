package com.bandage.v1.domain.band.repository

import com.bandage.v1.domain.band.model.Band
import com.bandage.v1.domain.band.model.BandApplication
import com.bandage.v1.domain.band.model.enums.ApplicationStatus
import com.bandage.v1.global.common.response.CursorResponse
import java.util.UUID

interface BandApplicationRepositoryCustom {
    fun findAllByPaging(
        lastId: UUID?,
        pageSize: Int,
        status: ApplicationStatus,
        band: Band,
    ): CursorResponse<BandApplication, UUID>
}
