package com.bandage.bandmanager.domain.performance.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "공연 소유권(OWNER) 양도 요청")
data class PerformanceOwnerDelegateRequest(
    @field:NotNull
    @Schema(description = "소유권을 넘겨받을 대상 멤버 ID (해당 공연의 MANAGER 여야 함)", example = "42")
    val targetMemberId: Long,
)
