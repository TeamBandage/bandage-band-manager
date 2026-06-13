package com.bandage.bandmanager.facade.dto

import com.bandage.bandmanager.domain.auth.dto.res.TokenDto
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "OAuth 로그인 응답")
data class OAuthLoginResponse(
    @Schema(description = "회원 인증용 access 토큰", example = "ey1234...")
    val accessToken: String,
    @Schema(description = "신규 가입 여부", example = "true")
    val isNewMember: Boolean,
    @Schema(hidden = true)
    val refreshToken: String,
) {
    companion object {
        fun of(
            tokens: TokenDto,
            isNewMember: Boolean,
        ): OAuthLoginResponse =
            OAuthLoginResponse(
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken,
                isNewMember = isNewMember,
            )
    }
}
