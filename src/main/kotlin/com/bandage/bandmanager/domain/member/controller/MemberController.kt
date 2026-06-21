package com.bandage.bandmanager.domain.member.controller

import com.bandage.bandmanager.domain.member.dto.req.MemberInfoUpdateRequest
import com.bandage.bandmanager.domain.member.dto.res.MemberInfoResponse
import com.bandage.bandmanager.domain.member.dto.res.MemberMetricsResponse
import com.bandage.bandmanager.domain.member.dto.res.MemberSearchItemResponse
import com.bandage.bandmanager.domain.member.service.MemberService
import com.bandage.bandmanager.facade.MemberJoinFacade
import com.bandage.bandmanager.facade.MemberMetricsFacade
import com.bandage.bandmanager.facade.MemberWithdrawFacade
import com.bandage.bandmanager.facade.dto.MemberJoinRequest
import com.bandage.bandmanager.facade.dto.MemberResponse
import com.bandage.bandmanager.global.common.constants.PathPrefix.PREFIX
import com.bandage.bandmanager.global.common.response.ApiResponse
import com.bandage.bandmanager.global.infra.s3.ImagePresignRequest
import com.bandage.bandmanager.global.infra.s3.ImagePresignResponse
import com.bandage.bandmanager.global.security.annotation.CurrentMemberId
import com.bandage.bandmanager.global.util.CookieUtil
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "members", description = "회원 API")
@RestController
@RequestMapping("$PREFIX/members")
class MemberController(
    private val memberService: MemberService,
    private val memberJoinFacade: MemberJoinFacade,
    private val memberWithdrawFacade: MemberWithdrawFacade,
    private val memberMetricsFacade: MemberMetricsFacade,
) {
    @PostMapping("/join")
    @Operation(operationId = "joinMember", summary = "회원 가입 API", description = "신규 회원을 생성합니다.")
    fun joinMember(
        @RequestBody request: MemberJoinRequest,
    ): ApiResponse<MemberResponse> =
        ApiResponse.success(
            memberJoinFacade.joinMember(request),
        )

    @GetMapping("/me")
    @Operation(operationId = "getMemberInfo", summary = "회원 정보 조회 API", description = "회원 본인의 정보를 조회합니다.")
    fun getMemberInfo(
        @CurrentMemberId memberId: Long,
    ): ApiResponse<MemberInfoResponse> =
        ApiResponse.success(
            memberService.getMemberInfo(memberId),
        )

    @PatchMapping("/me")
    @Operation(operationId = "updateMemberInfo", summary = "회원 기본 정보 변경 API", description = "회원 본인의 정보를 조회합니다.")
    fun updateMemberInfo(
        @Valid @RequestBody request: MemberInfoUpdateRequest,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        memberService.updateMemberInfo(request, memberId)
        return ApiResponse.success()
    }

    @PostMapping("/me/profile-image/presigned-url")
    @Operation(
        operationId = "issueMemberProfileImagePresignedUrl",
        summary = "회원 프로필 이미지 presigned URL 발급 API",
        description = "회원 본인 프로필 이미지를 S3에 PUT 업로드하기 위한 presigned URL을 발급합니다. 응답 objectKey 를 회원 정보 수정 시 profileImg 로 전달합니다.",
    )
    fun issueMemberProfileImagePresignedUrl(
        @Valid @RequestBody request: ImagePresignRequest,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<ImagePresignResponse> = ApiResponse.success(memberService.issueProfileImagePresignedUrl(request, memberId))

    @DeleteMapping("/me/profile-image")
    @Operation(
        operationId = "deleteMemberProfileImage",
        summary = "회원 프로필 이미지 삭제 API",
        description = "회원 본인의 프로필 이미지를 제거합니다. 이미지가 없어도 성공 처리합니다.",
    )
    fun deleteMemberProfileImage(
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        memberService.deleteProfileImage(memberId)
        return ApiResponse.success()
    }

    @GetMapping("/me/metrics")
    @Operation(operationId = "getMemberStats", summary = "회원 메트릭 조회 API", description = "본인의 밴드 수, 다가오는 합주/공연 수, 합주 세션 수를 조회합니다.")
    fun getMemberStats(
        @CurrentMemberId memberId: Long,
    ): ApiResponse<MemberMetricsResponse> = ApiResponse.success(memberMetricsFacade.getMemberMetrics(memberId))

    @GetMapping("/search")
    @Operation(
        operationId = "searchMembers",
        summary = "회원 검색 API",
        description = "이름/이메일 부분 일치 검색. 최대 20건. 본인은 결과에서 제외.",
    )
    fun searchMembers(
        @RequestParam(name = "q", required = false, defaultValue = "") q: String,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<List<MemberSearchItemResponse>> = ApiResponse.success(memberService.searchMembers(q, memberId))

    @DeleteMapping("/me")
    @Operation(operationId = "withdrawMember", summary = "회원 탈퇴 API", description = "회원 정보를 삭제하고 로그아웃 처리합니다.")
    fun withdrawMember(
        response: HttpServletResponse,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        memberWithdrawFacade.withdrawMember(memberId)
        response.setHeader(HttpHeaders.SET_COOKIE, CookieUtil.expireCookie())
        return ApiResponse.success()
    }
}
