package com.bandage.v1.domain.auth.controller

import com.bandage.v1.domain.auth.dto.req.MemberLoginRequest
import com.bandage.v1.domain.auth.dto.req.MemberPasswordChangeRequest
import com.bandage.v1.domain.auth.dto.res.MemberLoginResponse
import com.bandage.v1.domain.auth.service.MemberAuthService
import com.bandage.v1.global.common.constants.PathPrefix.PREFIX
import com.bandage.v1.global.common.response.ApiResponse
import com.bandage.v1.global.security.annotation.CurrentMemberId
import com.bandage.v1.global.util.CookieUtil
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "auth", description = "회원 인증 API")
@RestController
@RequestMapping("$PREFIX/auth")
class MemberAuthController(
    private val memberAuthService: MemberAuthService,
) {
    @PostMapping("/login")
    @Operation(operationId = "login", summary = "회원 로그인 API", description = "회원 로그인을 통해 access, refresh 토큰을 발급합니다.")
    fun login(
        @RequestBody request: MemberLoginRequest,
        response: HttpServletResponse,
    ): ApiResponse<MemberLoginResponse> {
        val tokens = memberAuthService.processLogin(request)
        response.setHeader(HttpHeaders.SET_COOKIE, CookieUtil.generateCookieFrom(tokens.refreshToken))
        return ApiResponse.success(
            MemberLoginResponse(tokens.accessToken),
        )
    }

    @DeleteMapping("/logout")
    @Operation(operationId = "logout", summary = "회원 로그아웃 API", description = "회원 로그아웃을 진행하고 redis, 쿠키에 저장된 refresh 토큰을 만료시킵니다.")
    fun logout(
        @CurrentMemberId memberId: Long,
        response: HttpServletResponse,
    ): ApiResponse<Unit> {
        memberAuthService.processLogout(memberId)
        response.setHeader(HttpHeaders.SET_COOKIE, CookieUtil.expireCookie())
        return ApiResponse.success()
    }

    @PostMapping("/refresh")
    @Operation(operationId = "tokenRefresh", summary = "토큰 리프레시 API", description = "refresh 토큰을 검증하고, 새로운 access, refresh 토큰을 발급합니다.")
    fun tokenRefresh(
        @CookieValue refreshToken: String,
        response: HttpServletResponse,
    ): ApiResponse<MemberLoginResponse> {
        val tokens = memberAuthService.reissueToken(refreshToken)
        response.setHeader(HttpHeaders.SET_COOKIE, CookieUtil.generateCookieFrom(tokens.refreshToken))
        return ApiResponse.success(
            MemberLoginResponse(tokens.accessToken),
        )
    }

    @PatchMapping("/password")
    @Operation(operationId = "changePassword", summary = "회원 비밀번호 변경 API", description = "회원 비밀번호를 변경합니다.")
    fun changePassword(
        @Valid @RequestBody request: MemberPasswordChangeRequest,
        @CurrentMemberId memberId: Long,
        response: HttpServletResponse,
    ): ApiResponse<Unit> {
        memberAuthService.changePassword(request, memberId)
        response.setHeader(HttpHeaders.SET_COOKIE, CookieUtil.expireCookie())
        return ApiResponse.success()
    }
}
