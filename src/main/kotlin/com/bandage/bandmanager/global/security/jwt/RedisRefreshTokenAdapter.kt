package com.bandage.bandmanager.global.security.jwt

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class RedisRefreshTokenAdapter(
    private val redisTemplate: RedisTemplate<String, String>,
) : RefreshTokenRepository {
    companion object {
        private const val RT_PREFIX = "RT:"
    }

    override fun save(
        memberId: Long,
        refreshToken: String,
        expiration: Long,
    ) {
        redisTemplate.opsForValue().set(
            getRtKey(memberId),
            refreshToken,
            Duration.ofMillis(expiration),
        )
    }

    override fun delete(memberId: Long) {
        redisTemplate.delete(getRtKey(memberId))
    }

    override fun get(memberId: Long): String? = redisTemplate.opsForValue().get(getRtKey(memberId))

    private fun getRtKey(memberId: Long): String = "$RT_PREFIX$memberId"
}
