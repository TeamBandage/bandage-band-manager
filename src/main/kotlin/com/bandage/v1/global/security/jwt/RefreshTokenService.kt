package com.bandage.v1.global.security.jwt

import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import com.bandage.v1.global.properties.JwtProperties
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class RefreshTokenService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val jwtProperties: JwtProperties,
    private val jwtProvider: JwtProvider,
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

    fun validateRefreshToken(
        refreshToken: String,
        memberId: Long,
    ) {
        if (!jwtProvider.validateToken(refreshToken)) throw BusinessException(ErrorCode.INVALID_REFRESH_TOKEN)
        val memberId = jwtProvider.getMemberIdFromToken(refreshToken)
        val savedRefreshToken = getRefreshToken(memberId) ?: throw BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN)
        if (refreshToken != savedRefreshToken) throw BusinessException(ErrorCode.INVALID_REFRESH_TOKEN)
    }

    fun deleteRefreshToken(memberId: Long) {
        redisTemplate.delete(getRtKey(memberId))
    }

    private fun getRtKey(memberId: Long): String = "$RT_PREFIX$memberId"

    private fun getRefreshToken(memberId: Long): String? = redisTemplate.opsForValue().get(getRtKey(memberId))
}
