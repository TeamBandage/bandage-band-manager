package com.bandage.v1.domain.auth.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Kakao OAuth 로그인 요청")
data class KakaoLoginRequest(
    @NotBlank
    @Schema(description = "Kakao authorize 단계에서 받은 authorization code", example = "abc123...")
    val code: String,
    @NotBlank
    @Schema(description = "FE 가 authorize 시 사용한 redirect URI. 카카오가 token 교환 시 동일성 검증")
    val redirectUri: String,
    @Schema(description = "(선택) FE CSRF state. BE 측 추가 검증이 필요한 경우만 사용")
    val state: String? = null,
)
