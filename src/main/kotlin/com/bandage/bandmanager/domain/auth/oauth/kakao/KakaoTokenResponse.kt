package com.bandage.bandmanager.domain.auth.oauth.kakao

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class KakaoTokenResponse(
    @field:JsonProperty("access_token") val accessToken: String?,
    @field:JsonProperty("token_type") val tokenType: String?,
    @field:JsonProperty("refresh_token") val refreshToken: String?,
    @field:JsonProperty("expires_in") val expiresIn: Long?,
    @field:JsonProperty("scope") val scope: String?,
)
