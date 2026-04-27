package com.bandage.v1.domain.performance.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import java.util.UUID

@Schema(description = "공연 참여 밴드 일괄 추가 요청")
data class PerformanceBandAddRequest(
    @field:NotEmpty
    @Schema(description = "추가할 밴드 ID 목록 (append 시맨틱)", example = "[\"550e8400-e29b-41d4-a716-446655440000\"]")
    val bandIds: List<UUID>,
)
