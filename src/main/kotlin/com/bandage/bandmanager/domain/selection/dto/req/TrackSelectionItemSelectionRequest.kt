package com.bandage.bandmanager.domain.selection.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "선곡 항목 선택/해제 요청")
data class TrackSelectionItemSelectionRequest(
    // 주의: Kotlin `is` 접두사 Boolean 은 Jackson 직렬화가 "selected" 로 떨어져
    // 요청 역직렬화와 키가 어긋난다(응답 TrackSelectionItemResponse.isSelected 도 "selected" 로 직렬화됨).
    // 요청/응답 키를 "selected" 로 통일하기 위해 필드명을 selected 로 둔다.
    @field:NotNull
    @Schema(description = "true=선택, false=해제")
    val selected: Boolean,
)
