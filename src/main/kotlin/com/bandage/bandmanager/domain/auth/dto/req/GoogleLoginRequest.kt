package com.bandage.bandmanager.domain.auth.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Google OAuth 로그인 요청")
data class GoogleLoginRequest(
    @NotBlank
    @Schema(description = "Google OAuth ID token (FE 에서 GIS 로 발급받은 JWT 형식 토큰)", example = "eyJhbGciOi...")
    val idToken: String,
)
