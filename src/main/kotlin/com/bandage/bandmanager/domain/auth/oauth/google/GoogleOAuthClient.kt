package com.bandage.bandmanager.domain.auth.oauth.google

import com.bandage.bandmanager.domain.auth.model.enums.ProviderType
import com.bandage.bandmanager.domain.auth.oauth.OAuthUserInfo
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import com.bandage.bandmanager.global.properties.GoogleOAuthProperties
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
class GoogleOAuthClient(
    private val properties: GoogleOAuthProperties,
) {
    private val restClient: RestClient = RestClient.create()

    fun fetchUserInfo(idToken: String): OAuthUserInfo {
        val tokenInfo = requestTokenInfo(idToken)
        verifyAudience(tokenInfo.aud)
        verifyIssuer(tokenInfo.iss)
        verifyEmail(tokenInfo.emailVerified)

        val sub =
            tokenInfo.sub
                ?: throw BusinessException(ErrorCode.OAUTH_TOKEN_INVALID)
        val email =
            tokenInfo.email
                ?: throw BusinessException(ErrorCode.OAUTH_TOKEN_INVALID)
        val name = tokenInfo.name ?: email.substringBefore("@")

        return OAuthUserInfo(
            provider = ProviderType.GOOGLE,
            providerId = sub,
            email = email,
            name = name,
            profileImg = tokenInfo.picture,
        )
    }

    private fun requestTokenInfo(idToken: String): GoogleTokenInfoResponse =
        try {
            restClient
                .get()
                .uri("${properties.tokenInfoUri}?id_token={idToken}", idToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError) { _, _ ->
                    throw BusinessException(ErrorCode.OAUTH_TOKEN_INVALID)
                }.onStatus(HttpStatusCode::is5xxServerError) { _, _ ->
                    throw BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR)
                }.body(GoogleTokenInfoResponse::class.java)
                ?: throw BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR)
        } catch (e: BusinessException) {
            throw e
        } catch (e: RestClientException) {
            throw BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR)
        }

    private fun verifyAudience(aud: String?) {
        if (aud != properties.clientId) {
            throw BusinessException(ErrorCode.OAUTH_TOKEN_INVALID)
        }
    }

    private fun verifyIssuer(iss: String?) {
        if (iss != "https://accounts.google.com" && iss != "accounts.google.com") {
            throw BusinessException(ErrorCode.OAUTH_TOKEN_INVALID)
        }
    }

    private fun verifyEmail(emailVerified: String?) {
        if (emailVerified?.lowercase() != "true") {
            throw BusinessException(ErrorCode.OAUTH_TOKEN_INVALID)
        }
    }
}
