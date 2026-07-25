package com.bandage.bandmanager.domain.selection.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "선곡 회의 매니저 권한 양도 요청")
data class TrackSelectionManagerTransferRequest(
    @field:NotNull
    @Schema(description = "새 매니저가 될 회원 ID. 회의 참여자여야 한다.")
    val managerId: Long,
)
