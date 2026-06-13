package com.bandage.bandmanager.domain.selection.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid

@Schema(description = "선곡 항목 부분 수정 요청")
data class TrackSelectionItemUpdateRequest(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    @Schema(description = "곡 길이(초 단위). 분/초(mm:ss) 표시 변환은 클라이언트에서 처리")
    val duration: Int? = null,
    @Schema(description = "참고 링크(예: YouTube)")
    val reference: String? = null,
    val note: String? = null,
    @field:Valid
    @Schema(description = "세션 정의 목록 (제공 시 전체 교체)")
    val sessions: List<SessionDefDto>? = null,
)
