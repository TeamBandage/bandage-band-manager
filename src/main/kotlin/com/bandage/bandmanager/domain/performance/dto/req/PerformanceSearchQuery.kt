package com.bandage.bandmanager.domain.performance.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.util.UUID

@Schema(description = "공연 검색 쿼리")
data class PerformanceSearchQuery(
    @field:NotBlank
    @Schema(description = "검색 키워드 (공연 이름)", example = "TuNA 정기공연")
    val keyword: String,
    @Schema(description = "마지막으로 조회된 공연 ID (커서)", example = "550e8400-e29b-41d4-a716-446655440000")
    val lastId: UUID?,
    @field:Min(1)
    @field:Max(100)
    @Schema(description = "페이지 크기", example = "10")
    val pageSize: Int = 10,
)
