package com.bandage.bandmanager.domain.member.dto.res

data class TokenDto(
    val accessToken: String,
    val refreshToken: String,
)
