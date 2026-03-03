package com.bandage.v1.domain.member.dto.res

data class TokenDto(
    val accessToken: String,
    val refreshToken: String,
)
