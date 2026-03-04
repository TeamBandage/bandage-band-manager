package com.bandage.v1.global.util

import org.springframework.http.ResponseCookie
import java.time.Duration

class CookieUtil {
    companion object {
        fun generateCookieFrom(refreshToken: String): String =
            ResponseCookie
                .from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofDays(14))
                .sameSite("None") // Lax 이상 권장
                .build()
                .toString()

        fun expireCookie(): String =
            ResponseCookie
                .from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build()
                .toString()
    }
}
