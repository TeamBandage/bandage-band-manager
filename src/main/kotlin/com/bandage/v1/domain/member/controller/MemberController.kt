package com.bandage.v1.domain.member.controller

import com.bandage.v1.domain.member.dto.req.MemberJoinRequest
import com.bandage.v1.domain.member.dto.res.MemberResponse
import com.bandage.v1.domain.member.service.MemberService
import com.bandage.v1.global.common.constants.PathPrefix.PREFIX
import com.bandage.v1.global.common.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "members", description = "회원 API")
@RestController
@RequestMapping("$PREFIX/members")
class MemberController(
    private val memberService: MemberService,
) {
    @PostMapping("/join")
    @Operation(summary = "회원 로그인 API", description = "회원 로그인을 통해 access, refresh 토큰을 발급합니다.")
    fun joinMember(
        @RequestBody request: MemberJoinRequest,
    ): ApiResponse<MemberResponse> = ApiResponse.success(memberService.createMember(request))
}
