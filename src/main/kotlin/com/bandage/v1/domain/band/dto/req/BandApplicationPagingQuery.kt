package com.bandage.v1.domain.band.dto.req

import com.bandage.v1.domain.band.model.enums.ApplicationStatus
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.UUID

data class BandApplicationPagingQuery(
    val lastId: UUID?,
    @field:Min(1) @field:Max(100)
    val pageSize: Int = 10,
    val status: ApplicationStatus = ApplicationStatus.PENDING,
)
