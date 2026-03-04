package com.bandage.v1.domain.member.dto.res

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "회원 로그인 응답")
data class MemberLoginResponse(
    @Schema(description = "회원 인증용 access 토큰", example = "ey1234...")
    val accessToken: String,
)
