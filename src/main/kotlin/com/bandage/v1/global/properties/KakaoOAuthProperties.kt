package com.bandage.v1.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "oauth.kakao")
data class KakaoOAuthProperties(
    val userInfoUri: String = "https://kapi.kakao.com/v2/user/me",
)
