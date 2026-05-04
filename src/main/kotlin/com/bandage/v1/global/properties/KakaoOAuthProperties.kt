package com.bandage.v1.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "oauth.kakao")
data class KakaoOAuthProperties(
    val userInfoUri: String = "https://kapi.kakao.com/v2/user/me",
    val tokenUri: String = "https://kauth.kakao.com/oauth/token",
    val clientId: String,
    val clientSecret: String? = null,
)
