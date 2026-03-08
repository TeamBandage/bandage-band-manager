package com.bandage.v1.global.security.jwt

data class TokenPayload(
    val memberId: Long,
    val role: String,
)
