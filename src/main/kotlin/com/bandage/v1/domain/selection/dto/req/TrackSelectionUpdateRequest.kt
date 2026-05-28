package com.bandage.v1.domain.selection.dto.req

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "선곡 수정 요청")
data class TrackSelectionUpdateRequest(
    @Schema(description = "회의 제목")
    val title: String?,
    @Schema(description = "변경할 매니저 회원 ID")
    val managerId: Long?,
)
