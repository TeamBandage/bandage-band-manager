package com.bandage.v1.domain.schedule.controller

import com.bandage.v1.domain.schedule.dto.req.ScheduleBoardCreateRequest
import com.bandage.v1.domain.schedule.dto.req.ScheduleBoardUpdateRequest
import com.bandage.v1.domain.schedule.dto.res.ScheduleBoardResponse
import com.bandage.v1.domain.schedule.dto.res.ScheduleConfirmResponse
import com.bandage.v1.domain.schedule.dto.res.ScheduleUnconfirmResponse
import com.bandage.v1.domain.schedule.service.ScheduleBoardService
import com.bandage.v1.facade.ScheduleConfirmFacade
import com.bandage.v1.global.common.constants.PathPrefix.PREFIX
import com.bandage.v1.global.common.response.ApiResponse
import com.bandage.v1.global.security.annotation.CurrentMemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "schedule-boards", description = "시간표 시안 API")
@RestController
@RequestMapping("$PREFIX/setlist-meetings/{meetingId}/schedule-boards")
class ScheduleBoardController(
    private val scheduleBoardService: ScheduleBoardService,
    private val scheduleConfirmFacade: ScheduleConfirmFacade,
) {
    @GetMapping
    @Operation(summary = "시간표 시안 목록", description = "회의 참여자만 호출 가능.")
    fun getBoards(
        @PathVariable meetingId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<List<ScheduleBoardResponse>> = ApiResponse.success(scheduleBoardService.getBoards(meetingId, memberId))

    @PostMapping
    @Operation(summary = "시간표 시안 생성", description = "매니저 권한, 회의당 최대 5개.")
    fun createBoard(
        @PathVariable meetingId: UUID,
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: ScheduleBoardCreateRequest,
    ): ApiResponse<ScheduleBoardResponse> = ApiResponse.success(scheduleBoardService.createBoard(meetingId, memberId, request))

    @PatchMapping("/{boardId}")
    @Operation(summary = "시간표 시안 수정", description = "매니저 권한, confirmed=true 인 시안은 수정 불가(409).")
    fun updateBoard(
        @PathVariable meetingId: UUID,
        @PathVariable boardId: UUID,
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: ScheduleBoardUpdateRequest,
    ): ApiResponse<ScheduleBoardResponse> = ApiResponse.success(scheduleBoardService.updateBoard(meetingId, boardId, memberId, request))

//    @PostMapping("/auto-suggest")
//    @Operation(summary = "시간표 시안 자동 생성", description = "합주 일정 블럭 자동 배치")
//    fun autoSuggestBoard(
//        @PathVariable meetingID: UUID,
//        @CurrentMemberId memberId: Long,
//        @RequestParam suggestionQty: Int,
//    ): ApiResponse<ScheduleBoardResponse> =
//        ApiResponse.success(scheduleBoardArrangeFacade.setupInitialScheduleBoard(meetingID, memberId, suggestionQty))

    @DeleteMapping("/{boardId}")
    @Operation(summary = "시간표 시안 삭제", description = "매니저 권한, confirmed=true 인 시안은 삭제 불가(409).")
    fun deleteBoard(
        @PathVariable meetingId: UUID,
        @PathVariable boardId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        scheduleBoardService.deleteBoard(meetingId, boardId, memberId)
        return ApiResponse.success()
    }

    @PostMapping("/{boardId}/confirm")
    @Operation(
        summary = "시간표 시안 확정",
        description =
            "매니저 권한. setlist meeting 이 lock 상태여야 하며, 같은 회의에 confirmed 시안이 이미 있으면 409. " +
                "확정 시 모든 ScheduleBlock 을 Practice 로 일괄 생성하고 (purpose=PERFORMANCE 면) PerformancePractice 링크 생성.",
    )
    fun confirmBoard(
        @PathVariable meetingId: UUID,
        @PathVariable boardId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<ScheduleConfirmResponse> = ApiResponse.success(scheduleConfirmFacade.confirmBoard(meetingId, boardId, memberId))

    @PostMapping("/{boardId}/unconfirm")
    @Operation(
        summary = "시간표 시안 확정 해제",
        description = "매니저 권한. 이미 생성된 Practice 는 유지되고 board.confirmed 만 false 로 토글.",
    )
    fun unconfirmBoard(
        @PathVariable meetingId: UUID,
        @PathVariable boardId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<ScheduleUnconfirmResponse> = ApiResponse.success(scheduleConfirmFacade.unconfirmBoard(meetingId, boardId, memberId))
}
