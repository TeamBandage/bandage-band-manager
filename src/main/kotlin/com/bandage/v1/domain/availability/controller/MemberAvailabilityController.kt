package com.bandage.v1.domain.availability.controller

import com.bandage.v1.domain.availability.dto.req.MemberAvailabilityRequest
import com.bandage.v1.domain.availability.dto.res.MemberAvailabilityResponse
import com.bandage.v1.domain.availability.service.MemberAvailabilityService
import com.bandage.v1.global.common.constants.PathPrefix.PREFIX
import com.bandage.v1.global.common.response.ApiResponse
import com.bandage.v1.global.security.annotation.CurrentMemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "member-availability", description = "멤버 글로벌 가용성 API")
@RestController
@RequestMapping("$PREFIX/me/availability")
class MemberAvailabilityController(
    private val memberAvailabilityService: MemberAvailabilityService,
) {
    @GetMapping
    @Operation(operationId = "getMyAvailability", summary = "내 가용성 조회", description = "본인의 주간 규칙/예외 가용성 조회. 미등록 시 빈 응답.")
    fun getMyAvailability(
        @CurrentMemberId memberId: Long,
    ): ApiResponse<MemberAvailabilityResponse> = ApiResponse.success(memberAvailabilityService.getMyAvailability(memberId))

    @PutMapping
    @Operation(operationId = "updateMyAvailability", summary = "내 가용성 등록/수정", description = "본인만 수정 가능. weeklyRules/exceptions 전체 교체.")
    fun updateMyAvailability(
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: MemberAvailabilityRequest,
    ): ApiResponse<MemberAvailabilityResponse> = ApiResponse.success(memberAvailabilityService.updateMyAvailability(memberId, request))
}
