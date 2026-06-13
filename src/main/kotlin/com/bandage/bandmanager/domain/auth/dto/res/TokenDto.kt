package com.bandage.bandmanager.domain.auth.dto.res

data class TokenDto(
    val accessToken: String,
    val refreshToken: String,
)
