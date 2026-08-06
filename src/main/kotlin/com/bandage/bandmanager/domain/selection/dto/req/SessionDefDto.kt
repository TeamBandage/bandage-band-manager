package com.bandage.bandmanager.domain.selection.dto.req

import com.bandage.bandmanager.global.common.domain.SessionSpec
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "세션 정의")
data class SessionDefDto(
    @Schema(
        description = "세션 식별자. 기존 세션을 유지·수정할 때만 응답에서 받은 값을 그대로 보낸다. 신규 세션은 생략(서버가 발급).",
        example = "0199c3f4-1b2c-7a3d-8e4f-5a6b7c8d9e0f",
        nullable = true,
    )
    val sessionId: String? = null,
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
