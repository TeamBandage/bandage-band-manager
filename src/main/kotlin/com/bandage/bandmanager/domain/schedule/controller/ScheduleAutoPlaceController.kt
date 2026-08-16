package com.bandage.bandmanager.domain.schedule.controller

import com.bandage.bandmanager.domain.schedule.dto.req.ScheduleAutoPlaceRequest
import com.bandage.bandmanager.domain.schedule.dto.res.ScheduleBlockResponse
import com.bandage.bandmanager.domain.schedule.service.ScheduleAutoPlaceService
import com.bandage.bandmanager.global.common.constants.PathPrefix.PREFIX
import com.bandage.bandmanager.global.common.response.ApiResponse
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
    fun autoSchedule(
        @PathVariable setlistId: UUID,
        @PathVariable boardId: UUID,
        @Valid @RequestBody request: ScheduleAutoPlaceRequest,
    ): ApiResponse<List<ScheduleBlockResponse>> =
        ApiResponse.success(scheduleAutoPlaceService.autoPlaceScheduleBlocks(setlistId, boardId, request))
}
