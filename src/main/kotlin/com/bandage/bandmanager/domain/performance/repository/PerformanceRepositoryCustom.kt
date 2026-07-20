package com.bandage.bandmanager.domain.performance.repository

import com.bandage.bandmanager.domain.performance.model.Performance
import com.bandage.bandmanager.global.common.response.CursorResponse
import java.time.LocalDate
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

    fun findMyPerformancesByCursor(
        memberId: Long,
        bandIds: List<UUID>,
        lastId: UUID?,
        pageSize: Int,
        from: LocalDate? = null,
        to: LocalDate? = null,
    ): CursorResponse<Performance, UUID>

    fun searchByTitleAndPaging(
        keyword: String,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Performance, UUID>
}
