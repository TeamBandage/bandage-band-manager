package com.bandage.v1.domain.schedule.controller

import com.bandage.v1.domain.schedule.dto.req.MemberScheduleRequest
import com.bandage.v1.domain.schedule.dto.res.MemberScheduleAggregateResponse
import com.bandage.v1.domain.schedule.dto.res.MemberScheduleResponse
import com.bandage.v1.domain.schedule.service.MemberScheduleService
import com.bandage.v1.global.common.constants.PathPrefix.PREFIX
import com.bandage.v1.global.common.response.ApiResponse
import com.bandage.v1.global.security.annotation.CurrentMemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(
    name = "schedule-member",
    description = "[DEPRECATED] 선곡 회의 멤버 가용 시간 API. 글로벌 가용성 API(/me/availability)로 대체 예정.",
)
@Deprecated(
    message = "회의 단위 MemberSchedule 은 글로벌 MemberAvailability(/me/availability)로 대체됩니다. 신규 연동 금지.",
    replaceWith = ReplaceWith("MemberAvailabilityController"),
)
@RestController
@RequestMapping("$PREFIX/setlist-meetings/{meetingId}/schedules")
class MemberScheduleController(
    private val memberScheduleService: MemberScheduleService,
) {
    @GetMapping("/me")
    @Operation(operationId = "getMySchedule", summary = "내 가용 시간 조회", description = "회의 참여자만 호출 가능. 미등록 시 빈 응답.")
    fun getMySchedule(
        @PathVariable meetingId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<MemberScheduleResponse> = ApiResponse.success(memberScheduleService.getMySchedule(meetingId, memberId))

    @PutMapping("/me")
    @Operation(operationId = "upsertMySchedule", summary = "내 가용 시간 등록/수정", description = "본인만 수정 가능. 날짜 중복 / practiceWindow 범위 검증.")
    fun upsertMySchedule(
        @PathVariable meetingId: UUID,
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: MemberScheduleRequest,
    ): ApiResponse<MemberScheduleResponse> = ApiResponse.success(memberScheduleService.upsertMySchedule(meetingId, memberId, request))

    @GetMapping
    @Operation(operationId = "getAllSchedules", summary = "참여자 가용 시간 목록", description = "회의 참여자만 호출 가능.")
    fun getAllSchedules(
        @PathVariable meetingId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<List<MemberScheduleResponse>> = ApiResponse.success(memberScheduleService.getAllSchedules(meetingId, memberId))

    @GetMapping("/aggregate")
    @Operation(operationId = "getAggregatedSchedule", summary = "가용 시간 집계", description = "날짜별 가용/불가용/미응답 카운트 + 응답 완료자 수.")
    fun getAggregatedSchedule(
        @PathVariable meetingId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<MemberScheduleAggregateResponse> = ApiResponse.success(memberScheduleService.getAggregatedSchedule(meetingId, memberId))
}
