package com.bandage.bandmanager.domain.jam.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "합주 세션 추가 요청")
data class JamSessionAddRequest(
    @field:NotBlank
    @Schema(description = "세션 토큰", example = "G-2")
    val sessionId: String,
    @field:NotBlank
    @Schema(
        description = "세션 이름. 영문 알파벳만 허용하며 서버가 대문자로 저장한다.",
        example = "GUITAR",
        pattern = "^[A-Za-z]+$",
    )
    val label: String,
    @Schema(description = "커스텀 세션 여부", example = "false")
    val custom: Boolean = false,
)
