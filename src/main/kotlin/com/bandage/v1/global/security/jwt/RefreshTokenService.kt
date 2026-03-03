package com.bandage.v1.global.security.jwt

import com.bandage.v1.global.properties.JwtProperties
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class RefreshTokenService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val jwtProperties: JwtProperties,
) {
    val refreshExpr = jwtProperties.refreshTokenExpr

    companion object {
        private const val RT_PREFIX = "RT:"
    }

    fun saveRefreshToken(
        memberId: Long,
        refreshToken: String,
    ) {
        redisTemplate.opsForValue().set(
            getRtKey(memberId),
            refreshToken,
            Duration.ofMillis(refreshExpr),
        )
    }

    fun getRefreshToken(memberId: Long): String? = redisTemplate.opsForValue().get(getRtKey(memberId))

    fun deleteRefreshToken(memberId: Long) {
        redisTemplate.delete(getRtKey(memberId))
    }

    private fun getRtKey(memberId: Long): String = "$RT_PREFIX$memberId"
}
