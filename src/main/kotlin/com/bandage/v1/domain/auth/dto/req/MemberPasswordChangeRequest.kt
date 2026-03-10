package com.bandage.v1.domain.auth.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "회원 비밀번호 변경 요청")
data class MemberPasswordChangeRequest(
    @NotBlank @Schema(description = "기존 비밀번호", example = "originalPW123!")
    val originalPassword: String,
    @NotBlank @Schema(description = "회원 이메일", example = "newPW123!")
    val newPassword: String,
)
