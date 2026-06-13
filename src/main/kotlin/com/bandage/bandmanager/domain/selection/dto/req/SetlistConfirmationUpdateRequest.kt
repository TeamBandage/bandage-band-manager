package com.bandage.bandmanager.domain.selection.dto.req

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "세션 확정/해제 요청")
data class SetlistConfirmationUpdateRequest(
    @Schema(description = "확정할 사용자 ID")
    val confirm: List<Long> = emptyList(),
    @Schema(description = "확정 해제할 사용자 ID")
    val unconfirm: List<Long> = emptyList(),
)
