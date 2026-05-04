package com.bandage.v1.domain.auth.oauth.google

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class GoogleTokenInfoResponse(
    val sub: String?,
    val aud: String?,
    val iss: String?,
    val email: String?,
    @JsonProperty("email_verified") val emailVerified: String?,
    val name: String?,
    val picture: String?,
    val exp: String?,
)
