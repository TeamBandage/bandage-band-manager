package com.bandage.v1.domain.auth.controller

import com.bandage.v1.domain.auth.dto.req.KakaoLoginRequest
import com.bandage.v1.domain.auth.dto.res.OAuthLoginApiResponse
import com.bandage.v1.domain.auth.oauth.kakao.KakaoOAuthClient
import com.bandage.v1.facade.OAuthLoginFacade
import com.bandage.v1.global.common.constants.PathPrefix.PREFIX
import com.bandage.v1.global.common.response.ApiResponse
import com.bandage.v1.global.util.CookieUtil
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "auth-oauth-kakao", description = "Kakao OAuth API")
@RestController
@RequestMapping("$PREFIX/auth/oauth/kakao")
class KakaoOAuthController(
    private val kakaoOAuthClient: KakaoOAuthClient,
    private val oAuthLoginFacade: OAuthLoginFacade,
) {
    @PostMapping
    @Operation(
        summary = "Kakao OAuth 로그인 / 회원가입 API",
        description =
            "FE 에서 Kakao SDK 로 발급받은 access token 을 검증하고, " +
                "신규 회원이면 가입 후 JWT 를 발급한다. 기존 회원이면 로그인 처리한다.",
    )
    fun loginWithKakao(
        @Valid @RequestBody request: KakaoLoginRequest,
        response: HttpServletResponse,
    ): ApiResponse<OAuthLoginApiResponse> {
        val userInfo = kakaoOAuthClient.fetchUserInfo(request.accessToken)
        val result = oAuthLoginFacade.loginOrJoin(userInfo)
        response.setHeader(HttpHeaders.SET_COOKIE, CookieUtil.generateCookieFrom(result.refreshToken))
        return ApiResponse.success(OAuthLoginApiResponse.of(result))
    }
}
