package com.bandage.v1.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val secret: String,
    val accessTokenExpr: Long,
    val refreshTokenExpr: Long,
)
