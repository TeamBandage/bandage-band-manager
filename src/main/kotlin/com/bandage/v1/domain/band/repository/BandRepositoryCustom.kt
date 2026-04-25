package com.bandage.v1.domain.band.repository

import com.bandage.v1.domain.band.model.Band
import com.bandage.v1.domain.band.model.enums.BandRole
import com.bandage.v1.global.common.response.CursorResponse
import java.util.UUID

interface BandRepositoryCustom {
    fun findAllByPaging(
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Band, UUID>

    fun findAllByMemberAndPaging(
        memberId: Long,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Band, UUID>

    fun findAllByMemberWithRoleAndPaging(
        memberId: Long,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<BandWithRole, UUID>

    fun searchByNameAndPaging(
        keyword: String,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Band, UUID>

    data class BandWithRole(
        val band: Band,
        val role: BandRole,
    )
}
