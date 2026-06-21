package com.bandage.bandmanager.domain.jam.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "합주 참여자 세션 변경 요청")
data class JamParticipantSessionUpdateRequest(
    @field:NotBlank
    @Schema(description = "변경할 세션 토큰(SessionDef.sessionId)", example = "G")
    val sessionId: String,
)
