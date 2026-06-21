package com.bandage.bandmanager.domain.performance.controller

import com.bandage.bandmanager.domain.performance.dto.req.PerformancePosterCreateRequest
import com.bandage.bandmanager.domain.performance.dto.res.PerformancePosterResponse
import com.bandage.bandmanager.domain.performance.service.PerformancePosterService
import com.bandage.bandmanager.global.common.constants.PathPrefix.PREFIX
import com.bandage.bandmanager.global.common.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "performance-posters", description = "공연 포스터 API")
@RestController
@RequestMapping("$PREFIX/performance-posters")
class PerformancePosterController(
    private val performancePosterService: PerformancePosterService,
) {
    @PostMapping
    @Operation(operationId = "createPerformancePoster", summary = "공연 포스터 등록 API", description = "공연에 신규 포스터를 등록합니다.")
    fun createPoster(
        @Valid @RequestBody request: PerformancePosterCreateRequest,
    ): ApiResponse<PerformancePosterResponse> = ApiResponse.success(performancePosterService.createPoster(request))

    @GetMapping
    @Operation(
        operationId = "getPerformancePosters",
        summary = "공연 포스터 목록 조회 API",
        description = "포스터 목록을 조회합니다. performanceId 제공 시 해당 공연의 포스터만, 미제공 시 전체 포스터를 조회합니다.",
    )
    fun getPosters(
        @RequestParam(required = false) performanceId: UUID?,
    ): ApiResponse<List<PerformancePosterResponse>> =
        if (performanceId != null) {
            ApiResponse.success(performancePosterService.getPostersByPerformance(performanceId))
        } else {
            ApiResponse.success(performancePosterService.getAllPosters())
        }

    @GetMapping("/{posterId}")
    @Operation(operationId = "getPerformancePoster", summary = "공연 포스터 단건 조회 API", description = "포스터 고유 식별 ID로 포스터를 조회합니다.")
    fun getPoster(
        @PathVariable posterId: UUID,
    ): ApiResponse<PerformancePosterResponse> = ApiResponse.success(performancePosterService.getPoster(posterId))

    @DeleteMapping("/{posterId}")
    @Operation(operationId = "deletePerformancePoster", summary = "공연 포스터 삭제 API", description = "포스터를 삭제합니다.")
    fun deletePoster(
        @PathVariable posterId: UUID,
    ): ApiResponse<Unit> {
        performancePosterService.deletePoster(posterId)
        return ApiResponse.success()
    }
}
