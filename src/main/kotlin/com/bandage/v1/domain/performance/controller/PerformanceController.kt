package com.bandage.v1.domain.performance.controller

import com.bandage.v1.domain.performance.dto.req.PerformanceCreateRequest
import com.bandage.v1.domain.performance.dto.req.PerformancePagingQuery
import com.bandage.v1.domain.performance.dto.req.PerformanceSearchQuery
import com.bandage.v1.domain.performance.dto.req.PerformanceSetlistAddRequest
import com.bandage.v1.domain.performance.dto.req.PerformanceUpdateRequest
import com.bandage.v1.domain.performance.dto.res.PerformanceDetailResponse
import com.bandage.v1.domain.performance.dto.res.PerformanceListResponse
import com.bandage.v1.domain.performance.dto.res.PerformanceResponse
import com.bandage.v1.domain.performance.dto.res.PerformanceSetlistResponse
import com.bandage.v1.domain.performance.service.PerformanceService
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
) {
    @PostMapping
    @Operation(summary = "공연 생성 API", description = "신규 공연을 생성하고 생성자를 매니저로 등록합니다.")
    fun createPerformance(
        @Valid @RequestBody request: PerformanceCreateRequest,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<PerformanceResponse> = ApiResponse.success(performanceService.createPerformance(request, memberId))

    @GetMapping
    @Operation(
        summary = "공연 목록 조회 API",
        description = "공연 목록을 커서 기반으로 조회합니다. bandId 제공 시 해당 밴드가 셋리스트로 참여하는 공연만 조회합니다.",
    )
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
    @Operation(summary = "내 공연 목록 조회 API", description = "본인이 속한 밴드가 셋리스트로 참여하는 공연 목록을 커서 기반으로 조회합니다.")
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

    @PostMapping("/{performanceId}/setlists/batch")
    @Operation(
        summary = "공연 참여 셋리스트 일괄 추가 API",
        description = "공연에 참여 셋리스트를 append 시맨틱으로 다중 추가합니다. PerformanceManager만 가능. 이미 등록된 셋리스트는 응답에서 제외.",
    )
    fun addSetlists(
        @PathVariable performanceId: UUID,
        @Valid @RequestBody request: PerformanceSetlistAddRequest,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<List<PerformanceSetlistResponse>> = ApiResponse.success(performanceService.addSetlists(performanceId, request, memberId))

    @DeleteMapping("/{performanceId}/setlists/{setlistId}")
    @Operation(summary = "공연 참여 셋리스트 단건 제거 API", description = "공연에서 특정 참여 셋리스트를 제거합니다. PerformanceManager만 가능.")
    fun removeSetlist(
        @PathVariable performanceId: UUID,
        @PathVariable setlistId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        performanceService.removeSetlist(performanceId, setlistId, memberId)
        return ApiResponse.success()
    }

    @DeleteMapping("/{performanceId}")
    @Operation(summary = "공연 삭제 API", description = "공연을 삭제합니다. PerformanceManager만 수행할 수 있습니다.")
    fun deletePerformance(
        @PathVariable performanceId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        performanceService.deletePerformance(performanceId, memberId)
        return ApiResponse.success()
    }
}
