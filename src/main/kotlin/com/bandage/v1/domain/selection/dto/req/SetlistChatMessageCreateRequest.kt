package com.bandage.v1.domain.selection.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "선곡 항목 채팅 메시지 작성 요청")
data class SetlistChatMessageCreateRequest(
    @field:NotBlank
    @field:Size(max = 500)
    @Schema(description = "메시지 본문 (최대 500자)")
    val message: String,
)
