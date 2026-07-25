package com.bandage.bandmanager.domain.jam.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "합주 세션 개별 수정 요청")
data class JamSessionUpdateRequest(
    @field:NotBlank
    @Schema(
        description = "세션 이름. 영문 알파벳만 허용하며 서버가 대문자로 저장한다.",
        example = "GUITAR",
        pattern = "^[A-Za-z]+$",
    )
    val label: String,
)
