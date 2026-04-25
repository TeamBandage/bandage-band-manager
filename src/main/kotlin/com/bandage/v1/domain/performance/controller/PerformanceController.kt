package com.bandage.v1.domain.performance.controller

import com.bandage.v1.domain.performance.dto.req.PerformanceCreateRequest
import com.bandage.v1.domain.performance.dto.req.PerformancePagingQuery
import com.bandage.v1.domain.performance.dto.req.PerformancePracticeAddRequest
import com.bandage.v1.domain.performance.dto.req.PerformancePracticeCreateRequest
import com.bandage.v1.domain.performance.dto.req.PerformanceSearchQuery
import com.bandage.v1.domain.performance.dto.req.PerformanceUpdateRequest
import com.bandage.v1.domain.performance.dto.res.PerformanceDetailResponse
import com.bandage.v1.domain.performance.dto.res.PerformanceListResponse
import com.bandage.v1.domain.performance.dto.res.PerformancePracticeResponse
import com.bandage.v1.domain.performance.dto.res.PerformanceResponse
import com.bandage.v1.domain.performance.service.PerformanceService
import com.bandage.v1.facade.PerformanceFacade
import com.bandage.v1.global.common.constants.PathPrefix.PREFIX
import com.bandage.v1.global.common.response.ApiResponse
import com.bandage.v1.global.common.response.CursorResponse
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "performances", description = "공연 API")
@RestController
@RequestMapping("$PREFIX/performances")
class PerformanceController(
    private val performanceService: PerformanceService,
    private val performanceFacade: PerformanceFacade,
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

    @GetMapping("/me")
    @Operation(summary = "내 공연 목록 조회 API", description = "본인이 속한 밴드가 참여하는 공연 목록을 커서 기반으로 조회합니다.")
    fun getMyPerformances(
        @Valid query: PerformancePagingQuery,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<CursorResponse<PerformanceListResponse, UUID>> =
        ApiResponse.success(performanceService.getMyPerformancesByCursor(memberId, query))

    @GetMapping("/search")
    @Operation(summary = "공연 검색 API", description = "공연 제목에 키워드가 포함된 공연을 커서 기반으로 조회합니다.")
    fun searchPerformances(
        @Valid query: PerformanceSearchQuery,
    ): ApiResponse<CursorResponse<PerformanceListResponse, UUID>> =
        ApiResponse.success(performanceService.searchPerformancesByCursor(query))

    @GetMapping("/{performanceId}")
    @Operation(summary = "공연 단건 조회 API", description = "공연 고유 식별 ID를 통해 공연 상세 정보를 조회합니다.")
    fun getPerformance(
        @PathVariable performanceId: UUID,
    ): ApiResponse<PerformanceDetailResponse> = ApiResponse.success(performanceService.getPerformanceDetail(performanceId))

    @PatchMapping("/{performanceId}")
    @Operation(summary = "공연 정보 수정 API", description = "공연 제목, 일정, 장소를 수정합니다. PerformanceManager만 수행할 수 있습니다.")
    fun updatePerformance(
        @PathVariable performanceId: UUID,
        @Valid @RequestBody request: PerformanceUpdateRequest,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        performanceService.updatePerformance(performanceId, request, memberId)
        return ApiResponse.success()
    }

    @PostMapping("/{performanceId}/practices")
    @Operation(summary = "공연 합주곡 추가 API (신규 생성)", description = "빈 합주를 즉시 생성하여 공연에 추가합니다. PerformanceManager만 수행할 수 있습니다.")
    fun addNewPractice(
        @PathVariable performanceId: UUID,
        @Valid @RequestBody request: PerformancePracticeCreateRequest,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<PerformancePracticeResponse> = ApiResponse.success(performanceFacade.addNewPractice(performanceId, request, memberId))

    @PostMapping("/{performanceId}/practices/batch")
    @Operation(summary = "공연 합주곡 리스트 추가 API", description = "기존 합주 ID 목록으로 공연에 합주를 일괄 추가합니다. PerformanceManager만 수행할 수 있습니다.")
    fun addPractices(
        @PathVariable performanceId: UUID,
        @Valid @RequestBody request: PerformancePracticeAddRequest,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<List<PerformancePracticeResponse>> =
        ApiResponse.success(performanceService.addPractices(performanceId, request, memberId))

    @DeleteMapping("/{performanceId}/practices/{practiceId}")
    @Operation(summary = "공연 합주곡 삭제 API", description = "공연에 연결된 합주를 제거합니다. PerformanceManager만 수행할 수 있습니다.")
    fun removePractice(
        @PathVariable performanceId: UUID,
        @PathVariable practiceId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        performanceService.removePractice(performanceId, practiceId, memberId)
        return ApiResponse.success()
    }

    @DeleteMapping("/{performanceId}")
    @Operation(summary = "공연 삭제 API", description = "공연을 삭제합니다. PerformanceManager만 수행할 수 있으며, 연관된 합주도 함께 삭제됩니다.")
    fun deletePerformance(
        @PathVariable performanceId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        performanceService.deletePerformance(performanceId, memberId)
        return ApiResponse.success()
    }
}
