package com.bandage.v1.domain.auth.oauth.kakao

import com.bandage.v1.domain.auth.model.enums.ProviderType
import com.bandage.v1.domain.auth.oauth.OAuthUserInfo
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import com.bandage.v1.global.properties.KakaoOAuthProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
class KakaoOAuthClient(
    private val properties: KakaoOAuthProperties,
    restClientBuilder: RestClient.Builder,
) {
    private val restClient: RestClient = restClientBuilder.build()

    fun fetchUserInfo(accessToken: String): OAuthUserInfo {
        val response =
            try {
                restClient
                    .get()
                    .uri(properties.userInfoUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                    .header(HttpHeaders.CONTENT_TYPE, "${MediaType.APPLICATION_FORM_URLENCODED_VALUE};charset=utf-8")
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError) { _, _ ->
                        throw BusinessException(ErrorCode.OAUTH_TOKEN_INVALID)
                    }.onStatus(HttpStatusCode::is5xxServerError) { _, _ ->
                        throw BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR)
                    }.body(KakaoUserResponse::class.java)
                    ?: throw BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR)
            } catch (e: BusinessException) {
                throw e
            } catch (e: RestClientException) {
                throw BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR)
            }

        val account =
            response.kakaoAccount
                ?: throw BusinessException(ErrorCode.OAUTH_TOKEN_INVALID)
        val email =
            account.email
                ?: throw BusinessException(ErrorCode.OAUTH_TOKEN_INVALID)
        val nickname = account.profile?.nickname ?: email.substringBefore("@")

        return OAuthUserInfo(
            provider = ProviderType.KAKAO,
            providerId = response.id.toString(),
            email = email,
            name = nickname,
            profileImg = account.profile?.profileImageUrl,
        )
    }
}
