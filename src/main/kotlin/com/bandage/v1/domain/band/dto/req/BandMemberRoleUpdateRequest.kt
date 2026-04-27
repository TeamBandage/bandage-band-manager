package com.bandage.v1.domain.band.dto.req

import com.bandage.v1.domain.band.model.enums.BandRole
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "밴드 멤버 역할 변경 요청")
data class BandMemberRoleUpdateRequest(
    @field:NotNull
    @Schema(description = "변경할 역할 (ADMIN | MEMBER). LEADER 위임은 별도 API 사용", example = "ADMIN")
    val role: BandRole,
)
