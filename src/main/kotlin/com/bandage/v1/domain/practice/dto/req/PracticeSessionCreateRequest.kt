package com.bandage.v1.domain.practice.dto.req

import com.bandage.v1.domain.practice.model.enums.SessionType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "합주 세션 생성 요청")
data class PracticeSessionCreateRequest(
    @NotBlank @Schema(description = "세션 레이블", example = "Guitar 2")
    val label: String,
    @Schema(description = "세션 타입", example = "GUITAR")
    val type: SessionType = SessionType.ETC,
)
