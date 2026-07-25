package com.bandage.bandmanager.domain.setlist.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "셋리스트 매니저 권한 양도 요청")
data class SetlistManagerTransferRequest(
    @field:NotNull
    @Schema(description = "새 매니저가 될 회원 ID. 셋리스트 접근 가능 멤버여야 한다.")
    val managerId: Long,
)
