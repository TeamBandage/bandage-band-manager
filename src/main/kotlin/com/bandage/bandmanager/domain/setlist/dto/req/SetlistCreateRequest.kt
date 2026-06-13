package com.bandage.bandmanager.domain.setlist.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.util.UUID

@Schema(description = "셋리스트 생성 요청")
data class SetlistCreateRequest(
    @field:NotNull
    @Schema(description = "원본 선곡(TrackSelection) ID")
    val trackSelectionId: UUID,
    @Schema(description = "셋리스트 제목 (생략 시 선곡 제목을 승계)")
    val title: String? = null,
)
