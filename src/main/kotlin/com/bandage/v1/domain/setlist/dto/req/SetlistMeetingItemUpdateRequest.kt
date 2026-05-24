package com.bandage.v1.domain.setlist.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid

@Schema(description = "선곡 회의 항목 부분 수정 요청")
data class SetlistMeetingItemUpdateRequest(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val duration: String? = null,
    val note: String? = null,
    @field:Valid
    @Schema(description = "세션 정의 목록 (제공 시 전체 교체)")
    val sessions: List<SessionDefDto>? = null,
)
