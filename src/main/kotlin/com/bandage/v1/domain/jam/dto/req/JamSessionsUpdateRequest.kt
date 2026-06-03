package com.bandage.v1.domain.jam.dto.req

import com.bandage.v1.domain.selection.dto.req.SessionDefDto
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid

@Schema(description = "합주 세션 정의 전체 교체 요청")
data class JamSessionsUpdateRequest(
    @field:Valid
    @Schema(description = "세션 정의 목록 (제공된 목록으로 전체 교체)")
    val sessions: List<SessionDefDto> = emptyList(),
)
