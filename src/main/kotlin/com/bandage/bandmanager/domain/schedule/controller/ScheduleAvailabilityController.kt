package com.bandage.bandmanager.domain.schedule.controller

import com.bandage.bandmanager.domain.schedule.dto.res.SlotAvailabilityResponse
import com.bandage.bandmanager.domain.schedule.service.ScheduleAvailabilityService
import com.bandage.bandmanager.global.common.constants.PathPrefix.PREFIX
import com.bandage.bandmanager.global.common.response.ApiResponse
import com.bandage.bandmanager.global.security.annotation.CurrentMemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@Tag(name = "schedule-availability", description = "시간표 슬롯별 멤버 가용 현황 API")
@RestController
@RequestMapping("$PREFIX/setlists/{setlistId}/slot-availabilities")
class ScheduleAvailabilityController(
    private val scheduleAvailabilityService: ScheduleAvailabilityService,
) {
    @GetMapping
    @Operation(
        operationId = "getSlotAvailabilities",
        summary = "슬롯별 멤버 가용 현황 조회",
        description =
            "셋리스트 참여자 권한. 조회 기간(from~to, 최대 31일)의 모든 슬롯에 대해 " +
                "가용/불가 멤버를 펼쳐 반환한다. 화면 단위(주/월)로 벌크 조회해 캐싱한 뒤 " +
                "슬롯 호버링 시 추가 호출 없이 표시하는 용도다. " +
                "특정 트랙 배치 시에는 그 트랙의 참여자와 availableMemberIds 를 교집합해 판정한다.",
    )
    fun getSlotAvailabilities(
        @PathVariable setlistId: UUID,
        @CurrentMemberId memberId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): ApiResponse<List<SlotAvailabilityResponse>> =
        ApiResponse.success(scheduleAvailabilityService.getSlotAvailabilities(setlistId, memberId, from, to))
}
