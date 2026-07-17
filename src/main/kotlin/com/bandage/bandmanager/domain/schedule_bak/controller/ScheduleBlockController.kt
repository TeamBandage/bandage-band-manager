package com.bandage.bandmanager.domain.schedule_bak.controller

import com.bandage.bandmanager.domain.schedule_bak.dto.req.ScheduleBlockPinRequest
import com.bandage.bandmanager.domain.schedule_bak.dto.req.ScheduleBlockUpsertRequest
import com.bandage.bandmanager.domain.schedule_bak.dto.res.ScheduleBlockResponse
import com.bandage.bandmanager.domain.schedule_bak.service.ScheduleBlockService
import com.bandage.bandmanager.global.common.constants.PathPrefix.PREFIX
import com.bandage.bandmanager.global.common.response.ApiResponse
import com.bandage.bandmanager.global.security.annotation.CurrentMemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "schedule-blocks", description = "시간표 블록 API (공연 단위)")
@RestController
@RequestMapping("$PREFIX/performances/{performanceId}/schedule-boards/{boardId}/blocks")
class ScheduleBlockController(
    private val scheduleBlockService: ScheduleBlockService,
) {
    @PutMapping("/{blockId}")
    @Operation(operationId = "upsertBlock", summary = "시간표 블록 등록/수정", description = "공연 매니저 권한, blockId 가 없으면 새로 생성, 있으면 갱신.")
    fun upsertBlock(
        @PathVariable performanceId: UUID,
        @PathVariable boardId: UUID,
        @PathVariable blockId: UUID,
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: ScheduleBlockUpsertRequest,
    ): ApiResponse<ScheduleBlockResponse> =
        ApiResponse.success(scheduleBlockService.upsertBlock(performanceId, boardId, blockId, memberId, request))

    @DeleteMapping("/{blockId}")
    @Operation(operationId = "deleteBlock", summary = "시간표 블록 삭제", description = "공연 매니저 권한, confirmed=true 인 시안의 블록은 삭제 불가.")
    fun deleteBlock(
        @PathVariable performanceId: UUID,
        @PathVariable boardId: UUID,
        @PathVariable blockId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        scheduleBlockService.deleteBlock(performanceId, boardId, blockId, memberId)
        return ApiResponse.success()
    }

    @PatchMapping("/{blockId}/pin")
    @Operation(operationId = "setPin", summary = "시간표 블록 핀 토글", description = "공연 매니저 권한, 요청 body 의 pinned 값으로 설정.")
    fun setPin(
        @PathVariable performanceId: UUID,
        @PathVariable boardId: UUID,
        @PathVariable blockId: UUID,
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: ScheduleBlockPinRequest,
    ): ApiResponse<ScheduleBlockResponse> =
        ApiResponse.success(scheduleBlockService.setPin(performanceId, boardId, blockId, memberId, request.pinned))
}
