package com.bandage.v1.domain.performance.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "공연 매니저 초대 요청")
data class PerformanceInvitationCreateRequest(
    @field:NotNull
    @Schema(description = "초대할 멤버 ID", example = "42")
    val memberId: Long,
)
