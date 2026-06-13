package com.bandage.bandmanager.domain.setlist.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.UUID

@Schema(description = "셋리스트 트랙 목록 조회 쿼리")
data class SetlistTrackPagingQuery(
    val lastId: UUID?,
    @field:Min(1)
    @field:Max(200)
    val pageSize: Int = 50,
)
