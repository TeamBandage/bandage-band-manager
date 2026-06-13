package com.bandage.bandmanager.domain.jam.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "합주 세션 참여자 추가 요청")
data class JamMemberAddRequest(
    @field:NotBlank
    @Schema(description = "세션 토큰(SessionDef.sessionId)", example = "G")
    val sessionId: String,
    @Schema(description = "추가할 회원 아이디", example = "1")
    val memberId: Long,
)
