package com.bandage.v1.global.security.jwt

interface RefreshTokenRepository {
    fun save(
        memberId: Long,
        refreshToken: String,
        expiration: Long,
    )

    fun delete(memberId: Long)

    fun get(memberId: Long): String?
}
