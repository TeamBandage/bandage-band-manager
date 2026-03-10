package com.bandage.v1.domain.band.dto.req

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.UUID

data class BandPagingQuery(
    val lastId: UUID?,
    @field:Min(1) @field:Max(100)
    val pageSize: Int = 10,
)
