package com.bandage.v1.domain.member.controller

import com.bandage.v1.domain.member.dto.req.MemberJoinRequest
import com.bandage.v1.domain.member.dto.res.MemberResponse
import com.bandage.v1.domain.member.service.MemberService
import com.bandage.v1.global.common.constants.PathPrefix.PREFIX
import com.bandage.v1.global.common.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
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
) {
    @PostMapping("/join")
    @Operation(summary = "회원 가입 API", description = "신규 회원을 생성합니다.")
    fun joinMember(
        @RequestBody request: MemberJoinRequest,
    ): ApiResponse<MemberResponse> = ApiResponse.success(memberService.createMember(request))

    @DeleteMapping
    @Operation(summary = "회원 탈퇴 API", description = "회원 정보를 삭제하고 로그아웃 처리합니다.")
    fun withdrawMember(): ApiResponse<Nothing> = ApiResponse.success()
}
