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

@Tag(name = "schedule-boards", description = "시간표 시안 API (공연 단위)")
@RestController
@RequestMapping("$PREFIX/performances/{performanceId}/schedule-boards")
class ScheduleBoardController(
    private val scheduleBoardService: ScheduleBoardService,
    private val scheduleConfirmFacade: ScheduleConfirmFacade,
) {
    @GetMapping
    @Operation(summary = "시간표 시안 목록", description = "공연 참여자만 호출 가능.")
    fun getBoards(
        @PathVariable performanceId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<List<ScheduleBoardResponse>> = ApiResponse.success(scheduleBoardService.getBoards(performanceId, memberId))

    @PostMapping
    @Operation(summary = "시간표 시안 생성", description = "공연 매니저 권한, 공연당 최대 5개.")
    fun createBoard(
        @PathVariable performanceId: UUID,
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: ScheduleBoardCreateRequest,
    ): ApiResponse<ScheduleBoardResponse> = ApiResponse.success(scheduleBoardService.createBoard(performanceId, memberId, request))

    @PatchMapping("/{boardId}")
    @Operation(summary = "시간표 시안 수정", description = "공연 매니저 권한, confirmed=true 인 시안은 수정 불가(409).")
    fun updateBoard(
        @PathVariable performanceId: UUID,
        @PathVariable boardId: UUID,
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: ScheduleBoardUpdateRequest,
    ): ApiResponse<ScheduleBoardResponse> = ApiResponse.success(scheduleBoardService.updateBoard(performanceId, boardId, memberId, request))

    @DeleteMapping("/{boardId}")
    @Operation(summary = "시간표 시안 삭제", description = "공연 매니저 권한, confirmed=true 인 시안은 삭제 불가(409).")
    fun deleteBoard(
        @PathVariable performanceId: UUID,
        @PathVariable boardId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        scheduleBoardService.deleteBoard(performanceId, boardId, memberId)
        return ApiResponse.success()
    }

    @PostMapping("/{boardId}/confirm")
    @Operation(
        summary = "시간표 시안 확정",
        description = "공연 매니저 권한. 같은 공연에 confirmed 시안이 이미 있으면 409. 확정 시 모든 ScheduleBlock 을 Jam 으로 일괄 생성.",
    )
    fun confirmBoard(
        @PathVariable performanceId: UUID,
        @PathVariable boardId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<ScheduleConfirmResponse> = ApiResponse.success(scheduleConfirmFacade.confirmBoard(performanceId, boardId, memberId))

    @PostMapping("/{boardId}/unconfirm")
    @Operation(
        summary = "시간표 시안 확정 해제",
        description = "공연 매니저 권한. 생성된 Jam 은 정리(soft-delete)되고 board.confirmed 가 false 로 토글.",
    )
    fun unconfirmBoard(
        @PathVariable performanceId: UUID,
        @PathVariable boardId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<ScheduleUnconfirmResponse> = ApiResponse.success(scheduleConfirmFacade.unconfirmBoard(performanceId, boardId, memberId))
}
