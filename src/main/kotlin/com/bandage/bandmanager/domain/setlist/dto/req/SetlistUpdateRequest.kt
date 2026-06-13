package com.bandage.bandmanager.domain.setlist.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "셋리스트 수정 요청")
data class SetlistUpdateRequest(
    @field:NotBlank
    @Schema(description = "변경할 셋리스트 제목")
    val title: String,
)
