package com.bandage.bandmanager.global.security.jwt

data class TokenPayload(
    val memberId: Long,
    val role: String,
)
