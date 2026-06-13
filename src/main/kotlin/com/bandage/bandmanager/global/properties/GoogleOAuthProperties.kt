package com.bandage.bandmanager.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "oauth.google")
data class GoogleOAuthProperties(
    val tokenInfoUri: String = "https://oauth2.googleapis.com/tokeninfo",
    val clientId: String,
)
