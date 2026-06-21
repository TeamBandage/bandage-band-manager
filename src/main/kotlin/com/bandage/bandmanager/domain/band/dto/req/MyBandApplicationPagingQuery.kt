package com.bandage.bandmanager.domain.band.dto.req

import com.bandage.bandmanager.domain.band.model.enums.ApplicationStatus
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.UUID

data class MyBandApplicationPagingQuery(
    val lastId: UUID?,
    @field:Min(1) @field:Max(100)
    val pageSize: Int = 10,
    // 옵션: 지정 시 해당 상태만, 미지정 시 전체 상태 조회
    val status: ApplicationStatus? = null,
)
