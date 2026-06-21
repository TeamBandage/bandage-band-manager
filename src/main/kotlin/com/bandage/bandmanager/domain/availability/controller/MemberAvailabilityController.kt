package com.bandage.bandmanager.domain.availability.controller

import com.bandage.bandmanager.domain.availability.dto.req.MemberAvailabilityRequest
import com.bandage.bandmanager.domain.availability.dto.res.MemberAvailabilityResponse
import com.bandage.bandmanager.domain.availability.service.MemberAvailabilityService
import com.bandage.bandmanager.global.common.constants.PathPrefix.PREFIX
import com.bandage.bandmanager.global.common.response.ApiResponse
import com.bandage.bandmanager.global.common.response.ScheduleSlotResponse
import com.bandage.bandmanager.global.security.annotation.CurrentMemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

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

    @GetMapping("/slots")
    @Operation(
        operationId = "getMyAvailabilitySlots",
        summary = "내 가용성 슬롯 조회",
        description = "조회 기간(from~to, 최대 366일)에 속한 가용 슬롯을 날짜별로 전개해 반환. 클라이언트 추가 계산 불필요.",
    )
    fun getMyAvailabilitySlots(
        @CurrentMemberId memberId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): ApiResponse<List<ScheduleSlotResponse>> = ApiResponse.success(memberAvailabilityService.getMySlots(memberId, from, to))

    @PutMapping
    @Operation(
        operationId = "updateMyAvailability",
        summary = "내 가용성 등록/수정",
        description = "본인만 수정 가능. effectiveFrom~effectiveTo 구간만 교체하고 구간 밖(과거 포함)은 보존한다.",
    )
    fun updateMyAvailability(
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: MemberAvailabilityRequest,
    ): ApiResponse<MemberAvailabilityResponse> = ApiResponse.success(memberAvailabilityService.updateMyAvailability(memberId, request))
}
