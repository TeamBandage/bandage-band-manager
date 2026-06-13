package com.bandage.bandmanager.domain.performance.repository

import com.bandage.bandmanager.domain.performance.model.Performance
import com.bandage.bandmanager.global.common.response.CursorResponse
import java.util.UUID

interface PerformanceRepositoryCustom {
    fun findAllByPaging(
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Performance, UUID>

    fun findAllByBandIdAndPaging(
        bandId: UUID,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Performance, UUID>

    fun findAllByBandIdsAndPaging(
        bandIds: List<UUID>,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Performance, UUID>

    fun searchByTitleAndPaging(
        keyword: String,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Performance, UUID>
}
