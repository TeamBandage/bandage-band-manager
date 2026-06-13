package com.bandage.bandmanager.domain.auth.dto.res

import com.bandage.bandmanager.facade.dto.OAuthLoginResponse
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "OAuth 로그인 API 응답")
data class OAuthLoginApiResponse(
    @Schema(description = "회원 인증용 access 토큰", example = "ey1234...")
    val accessToken: String,
    @Schema(description = "신규 가입 여부 (true: 회원가입 + 로그인, false: 기존 회원 로그인)", example = "true")
    val isNewMember: Boolean,
) {
    companion object {
        fun of(response: OAuthLoginResponse): OAuthLoginApiResponse =
            OAuthLoginApiResponse(
                accessToken = response.accessToken,
                isNewMember = response.isNewMember,
            )
    }
}
