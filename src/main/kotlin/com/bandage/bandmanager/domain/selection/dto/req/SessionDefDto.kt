package com.bandage.bandmanager.domain.selection.dto.req

import com.bandage.bandmanager.global.common.domain.SessionDef
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

@Schema(description = "세션 정의")
data class SessionDefDto(
    @field:NotBlank
    @Schema(description = "세션 토큰", example = "G")
    val sessionId: String,
    @field:NotBlank
    @Schema(description = "세션 이름", example = "기타")
    val label: String,
    @field:NotBlank
    @Schema(description = "표시용 약어", example = "G")
    val short: String,
    @field:Min(1)
    @Schema(description = "정원", example = "1")
    val need: Int,
    @Schema(description = "커스텀 세션 여부", example = "false")
    val custom: Boolean = false,
) {
    fun toEntity(): SessionDef = SessionDef(sessionId = sessionId, label = label, short = short, need = need, custom = custom)
}
