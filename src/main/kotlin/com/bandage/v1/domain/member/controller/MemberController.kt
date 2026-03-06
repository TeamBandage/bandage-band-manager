package com.bandage.v1.domain.member.controller

import com.bandage.v1.domain.member.service.MemberService
import com.bandage.v1.facade.MemberJoinFacade
import com.bandage.v1.facade.MemberWithdrawFacade
import com.bandage.v1.facade.dto.MemberJoinRequest
import com.bandage.v1.facade.dto.MemberResponse
import com.bandage.v1.global.common.constants.PathPrefix.PREFIX
import com.bandage.v1.global.common.response.ApiResponse
import com.bandage.v1.global.util.CookieUtil
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "members", description = "회원 API")
@RestController
@RequestMapping("$PREFIX/members")
class MemberController(
    private val memberService: MemberService,
    private val memberJoinFacade: MemberJoinFacade,
    private val memberWithdrawFacade: MemberWithdrawFacade,
) {
    @PostMapping("/join")
    @Operation(summary = "회원 가입 API", description = "신규 회원을 생성합니다.")
    fun joinMember(
        @RequestBody request: MemberJoinRequest,
    ): ApiResponse<MemberResponse> =
        ApiResponse.success(
            memberJoinFacade.joinMember(request),
        )

    @DeleteMapping
    @Operation(summary = "회원 탈퇴 API", description = "회원 정보를 삭제하고 로그아웃 처리합니다.")
    fun withdrawMember(response: HttpServletResponse): ApiResponse<Nothing> {
        memberWithdrawFacade.withdrawMember()
        response.setHeader(HttpHeaders.SET_COOKIE, CookieUtil.expireCookie())
        return ApiResponse.success()
    }
}
