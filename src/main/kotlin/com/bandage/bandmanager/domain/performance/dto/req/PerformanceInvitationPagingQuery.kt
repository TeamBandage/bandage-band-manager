package com.bandage.bandmanager.domain.performance.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.UUID

@Schema(description = "공연 초대 목록 조회 쿼리")
data class PerformanceInvitationPagingQuery(
    @Schema(description = "마지막으로 조회된 초대 ID (커서)", example = "550e8400-e29b-41d4-a716-446655440000")
    val lastId: UUID?,
    @field:Min(1)
    @field:Max(100)
    @Schema(description = "페이지 크기", example = "20")
    val pageSize: Int = 20,
)
