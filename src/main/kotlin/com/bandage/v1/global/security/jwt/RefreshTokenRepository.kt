package com.bandage.v1.global.security.jwt

interface RefreshTokenRepository {
    fun save(
        memberId: Long,
        refreshToken: String,
    )

    fun delete(memberId: Long)

    fun validate(
        refreshToken: String,
        memberId: Long,
    )
}
