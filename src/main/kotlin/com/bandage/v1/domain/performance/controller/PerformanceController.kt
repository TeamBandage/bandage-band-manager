package com.bandage.v1.domain.performance.controller

import com.bandage.v1.domain.performance.dto.req.PerformanceCreateRequest
import com.bandage.v1.domain.performance.dto.req.PerformancePagingQuery
import com.bandage.v1.domain.performance.dto.res.PerformanceListResponse
import com.bandage.v1.domain.performance.dto.res.PerformanceResponse
import com.bandage.v1.domain.performance.service.PerformanceService
import com.bandage.v1.global.common.constants.PathPrefix.PREFIX
import com.bandage.v1.global.common.response.ApiResponse
import com.bandage.v1.global.common.response.CursorResponse
import com.bandage.v1.global.security.annotation.CurrentMemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "performances", description = "공연 API")
@RestController
@RequestMapping("$PREFIX/performances")
class PerformanceController(
    private val performanceService: PerformanceService,
) {
    @PostMapping
    @Operation(summary = "공연 생성 API", description = "신규 공연을 생성하고 생성자를 매니저로 등록합니다.")
    fun createPerformance(
        @Valid @RequestBody request: PerformanceCreateRequest,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<PerformanceResponse> = ApiResponse.success(performanceService.createPerformance(request, memberId))

    @GetMapping
    @Operation(summary = "공연 목록 조회 API", description = "공연 목록을 커서 기반으로 조회합니다. bandId 제공 시 해당 밴드 소속 공연만 조회합니다.")
    fun getPerformances(
        @RequestParam(required = false) bandId: UUID?,
        @Valid query: PerformancePagingQuery,
    ): ApiResponse<CursorResponse<PerformanceListResponse, UUID>> =
        if (bandId != null) {
            ApiResponse.success(performanceService.getPerformancesByBand(bandId, query))
        } else {
            ApiResponse.success(performanceService.getPerformances(query))
        }
}
