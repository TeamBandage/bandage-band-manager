package com.bandage.bandmanager.domain.schedule.controller

import com.bandage.bandmanager.domain.schedule.dto.req.ScheduleAutoPlaceRequest
import com.bandage.bandmanager.domain.schedule.dto.res.ScheduleBoardResponse
import com.bandage.bandmanager.domain.schedule.service.ScheduleAutoPlaceService
import com.bandage.bandmanager.global.common.constants.PathPrefix.PREFIX
import com.bandage.bandmanager.global.common.response.ApiResponse
import com.bandage.bandmanager.global.security.annotation.CurrentMemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "schedule-auto-place", description = "시간표 자동 배치 API")
@RestController
@RequestMapping("$PREFIX/setlists/{setlistId}/schedule-boards/{boardId}/auto-schedule")
class ScheduleAutoPlaceController(
    private val scheduleAutoPlaceService: ScheduleAutoPlaceService,
) {
    @PostMapping
    @Operation(
        operationId = "autoSchedule",
        summary = "시간표 자동 배치",
        description = "셋리스트 매니저 권한, confirmed=true 인 시안은 배치 불가(409).",
    )
    fun autoSchedule(
        @PathVariable setlistId: UUID,
        @PathVariable boardId: UUID,
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: ScheduleAutoPlaceRequest,
    ): ApiResponse<ScheduleBoardResponse> =
        ApiResponse.success(scheduleAutoPlaceService.autoPlaceScheduleBlocks(setlistId, boardId, memberId, request))
}
