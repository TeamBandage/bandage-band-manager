package com.bandage.v1.domain.setlist.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.UUID

@Schema(description = "셋리스트 목록 조회 쿼리")
data class SetlistPagingQuery(
    @Schema(description = "마지막으로 조회된 셋리스트 ID (커서)")
    val lastId: UUID?,
    @field:Min(1)
    @field:Max(100)
    @Schema(description = "페이지 크기", example = "20")
    val pageSize: Int = 20,
)
