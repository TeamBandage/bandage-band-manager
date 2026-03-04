package com.bandage.v1.domain.auth.dto.res

data class TokenDto(
    val accessToken: String,
    val refreshToken: String,
)
