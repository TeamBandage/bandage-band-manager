package com.bandage.bandmanager.domain.performance.controller

import com.bandage.bandmanager.domain.performance.dto.req.PerformancePosterCreateRequest
import com.bandage.bandmanager.domain.performance.dto.req.PerformancePosterPresignRequest
import com.bandage.bandmanager.domain.performance.dto.req.PerformancePosterUpdateRequest
import com.bandage.bandmanager.domain.performance.dto.res.PerformancePosterResponse
import com.bandage.bandmanager.domain.performance.service.PerformancePosterService
import com.bandage.bandmanager.global.common.constants.PathPrefix.PREFIX
import com.bandage.bandmanager.global.common.response.ApiResponse
import com.bandage.bandmanager.global.infra.s3.ImagePresignResponse
import com.bandage.bandmanager.global.security.annotation.CurrentMemberId
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "performance-posters", description = "공연 포스터 API")
@RestController
@RequestMapping("$PREFIX/performance-posters")
class PerformancePosterController(
    private val performancePosterService: PerformancePosterService,
) {
    @PostMapping("/presigned-url")
    @Operation(
        operationId = "issuePerformancePosterPresignedUrl",
        summary = "공연 포스터 presigned URL 발급 API",
        description = "포스터 이미지를 S3에 PUT 업로드하기 위한 presigned URL을 발급합니다. OWNER/MANAGER만 가능. 응답 objectKey 를 포스터 등록 시 imageKey 로 전달합니다.",
    )
    fun issuePresignedUrl(
        @RequestParam performanceId: UUID,
        @Valid @RequestBody request: PerformancePosterPresignRequest,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<ImagePresignResponse> = ApiResponse.success(performancePosterService.issuePresignedUrl(performanceId, request, memberId))

    @PostMapping
    @Operation(operationId = "createPerformancePoster", summary = "공연 포스터 등록 API", description = "공연에 신규 포스터를 등록합니다. OWNER/MANAGER만 가능.")
    fun createPoster(
        @Valid @RequestBody request: PerformancePosterCreateRequest,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<PerformancePosterResponse> = ApiResponse.success(performancePosterService.createPoster(request, memberId))

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

    @PatchMapping("/{posterId}")
    @Operation(
        operationId = "updatePerformancePoster",
        summary = "공연 포스터 설명 수정 API",
        description = "포스터 설명을 수정합니다. description=null 전달 시 설명을 제거합니다. OWNER/MANAGER만 가능.",
    )
    fun updatePoster(
        @PathVariable posterId: UUID,
        @Valid @RequestBody request: PerformancePosterUpdateRequest,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<PerformancePosterResponse> = ApiResponse.success(performancePosterService.updateDescription(posterId, request, memberId))

    @DeleteMapping("/{posterId}")
    @Operation(operationId = "deletePerformancePoster", summary = "공연 포스터 삭제 API", description = "포스터를 삭제합니다. OWNER/MANAGER만 가능.")
    fun deletePoster(
        @PathVariable posterId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        performancePosterService.deletePoster(posterId, memberId)
        return ApiResponse.success()
    }
}
