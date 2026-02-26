package com.bandage.v1.domain.band.repository

import com.bandage.v1.domain.band.model.Band
import com.bandage.v1.global.common.response.CursorResponse
import java.util.UUID

interface BandRepositoryCustom {
    fun findAllByPaging(
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Band, UUID>
}
