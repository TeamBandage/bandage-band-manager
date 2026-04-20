package com.bandage.v1.domain.performance.controller

import com.bandage.v1.domain.performance.dto.req.PerformanceCreateRequest
import com.bandage.v1.domain.performance.dto.res.PerformanceResponse
import com.bandage.v1.domain.performance.service.PerformanceService
import com.bandage.v1.global.common.constants.PathPrefix.PREFIX
import com.bandage.v1.global.common.response.ApiResponse
import com.bandage.v1.global.security.annotation.CurrentMemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
}
