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
        // jwt.refresh-token-expr 은 초 단위다(JwtProvider 도 * 1000 으로 변환해 쓴다).
        // ofMillis 로 넘기면 7시간이 25초가 되어 재발급이 EXPIRED_REFRESH_TOKEN 으로 실패한다.
        redisTemplate.opsForValue().set(
            getRtKey(memberId),
            refreshToken,
            Duration.ofSeconds(expiration),
        )
    }

    override fun delete(memberId: Long) {
        redisTemplate.delete(getRtKey(memberId))
    }

    override fun get(memberId: Long): String? = redisTemplate.opsForValue().get(getRtKey(memberId))

    private fun getRtKey(memberId: Long): String = "$RT_PREFIX$memberId"
}
