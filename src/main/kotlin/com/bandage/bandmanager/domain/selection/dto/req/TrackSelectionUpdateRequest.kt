package com.bandage.bandmanager.domain.selection.dto.req

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "선곡 수정 요청")
data class TrackSelectionUpdateRequest(
    @Schema(description = "회의 제목")
    val title: String?,
)
