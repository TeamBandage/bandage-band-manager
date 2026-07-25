package com.bandage.bandmanager.domain.selection.dto.req

import com.bandage.bandmanager.global.common.domain.SessionSpec
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "세션 정의")
data class SessionDefDto(
    @field:NotBlank
    @Schema(description = "세션 토큰", example = "G")
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
) {
    fun toSpec(): SessionSpec = SessionSpec(sessionId = sessionId, label = label, custom = custom)
}
