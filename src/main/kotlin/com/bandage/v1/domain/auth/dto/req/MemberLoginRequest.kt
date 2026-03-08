package com.bandage.v1.domain.auth.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "회원 로그인 요청")
data class MemberLoginRequest(
    @NotBlank @Schema(description = "회원 이메일", example = "member@google.com")
    val email: String,
    @NotBlank @Schema(description = "회원 비밀번호", example = "pw1234")
    val password: String,
)
