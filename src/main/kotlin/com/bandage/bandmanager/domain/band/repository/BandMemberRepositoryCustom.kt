package com.bandage.bandmanager.domain.band.repository

import com.bandage.bandmanager.domain.band.model.Band
import com.bandage.bandmanager.domain.band.model.BandMember
import com.bandage.bandmanager.global.common.response.CursorResponse
import java.util.UUID

interface BandMemberRepositoryCustom {
    fun findAllByPaging(
        lastId: UUID?,
        pageSize: Int,
        band: Band,
    ): CursorResponse<BandMember, UUID>
}
