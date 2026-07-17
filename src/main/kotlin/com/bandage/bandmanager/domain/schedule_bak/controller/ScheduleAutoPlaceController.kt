package com.bandage.bandmanager.domain.schedule_bak.controller

import com.bandage.bandmanager.domain.schedule_bak.dto.req.AutoPlaceRequest
import com.bandage.bandmanager.domain.schedule_bak.dto.res.ProposalResponse
import com.bandage.bandmanager.facade.ScheduleAutoPlaceFacade
import com.bandage.bandmanager.global.common.constants.PathPrefix.PREFIX
import com.bandage.bandmanager.global.common.response.ApiResponse
import com.bandage.bandmanager.global.security.annotation.CurrentMemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "schedule-auto-place", description = "시간표 자동 배치 API")
@RestController
@RequestMapping("$PREFIX/performances/{performanceId}/schedule-boards/{boardId}")
class ScheduleAutoPlaceController(
    private val scheduleAutoPlaceFacade: ScheduleAutoPlaceFacade,
) {
    @PostMapping("/preview")
    @Operation(operationId = "preview", summary = "자동 배치 미리보기", description = "공연 참여자. 저장하지 않고 배치 제안만 반환.")
    fun preview(
        @PathVariable performanceId: UUID,
        @PathVariable boardId: UUID,
        @CurrentMemberId memberId: Long,
        @RequestBody request: AutoPlaceRequest,
    ): ApiResponse<ProposalResponse> = ApiResponse.success(scheduleAutoPlaceFacade.preview(performanceId, boardId, memberId, request))

    @PostMapping("/auto-place")
    @Operation(operationId = "autoPlace", summary = "자동 배치 실행", description = "공연 매니저. 비고정 블록을 제거하고 제안을 블록으로 저장.")
    fun autoPlace(
        @PathVariable performanceId: UUID,
        @PathVariable boardId: UUID,
        @CurrentMemberId memberId: Long,
        @RequestBody request: AutoPlaceRequest,
    ): ApiResponse<ProposalResponse> = ApiResponse.success(scheduleAutoPlaceFacade.autoPlace(performanceId, boardId, memberId, request))

    @PostMapping("/replan")
    @Operation(operationId = "replan", summary = "재배치", description = "공연 매니저. 고정(pinned/anchored) 블록은 유지하고 나머지를 재배치.")
    fun replan(
        @PathVariable performanceId: UUID,
        @PathVariable boardId: UUID,
        @CurrentMemberId memberId: Long,
        @RequestBody request: AutoPlaceRequest,
    ): ApiResponse<ProposalResponse> = ApiResponse.success(scheduleAutoPlaceFacade.replan(performanceId, boardId, memberId, request))

    @PatchMapping("/blocks/{blockId}/anchor")
    @Operation(operationId = "anchorBlock", summary = "블록 고정(anchor)", description = "공연 매니저. 블록을 고정하여 재배치 시 유지되도록 함.")
    fun anchorBlock(
        @PathVariable performanceId: UUID,
        @PathVariable boardId: UUID,
        @PathVariable blockId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        scheduleAutoPlaceFacade.anchorBlock(performanceId, boardId, blockId, memberId)
        return ApiResponse.success()
    }
}
