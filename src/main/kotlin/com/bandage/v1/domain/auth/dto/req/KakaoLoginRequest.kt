package com.bandage.v1.domain.auth.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Kakao OAuth 로그인 요청")
data class KakaoLoginRequest(
    @NotBlank
    @Schema(description = "Kakao OAuth access token (FE에서 카카오 SDK로 발급받은 토큰)", example = "AAAA...")
    val accessToken: String,
)
