package com.bandage.bandmanager.domain.performance.controller

import com.bandage.bandmanager.domain.performance.dto.req.PerformanceCreateRequest
import com.bandage.bandmanager.domain.performance.dto.req.PerformanceInvitationCreateRequest
import com.bandage.bandmanager.domain.performance.dto.req.PerformanceInvitationPagingQuery
import com.bandage.bandmanager.domain.performance.dto.req.PerformanceOwnerDelegateRequest
import com.bandage.bandmanager.domain.performance.dto.req.PerformancePagingQuery
import com.bandage.bandmanager.domain.performance.dto.req.PerformanceSearchQuery
import com.bandage.bandmanager.domain.performance.dto.req.PerformanceSetlistAddRequest
import com.bandage.bandmanager.domain.performance.dto.req.PerformanceUpdateRequest
import com.bandage.bandmanager.domain.performance.dto.res.PerformanceDetailResponse
import com.bandage.bandmanager.domain.performance.dto.res.PerformanceInvitationResponse
import com.bandage.bandmanager.domain.performance.dto.res.PerformanceListResponse
import com.bandage.bandmanager.domain.performance.dto.res.PerformanceResponse
import com.bandage.bandmanager.domain.performance.dto.res.PerformanceSetlistResponse
import com.bandage.bandmanager.domain.performance.dto.res.PerformanceSetlistTracksResponse
import com.bandage.bandmanager.domain.performance.model.enums.PerformanceInvitationStatus
import com.bandage.bandmanager.domain.performance.service.PerformanceService
import com.bandage.bandmanager.facade.PerformanceSetlistTrackFacade
import com.bandage.bandmanager.global.common.constants.PathPrefix.PREFIX
import com.bandage.bandmanager.global.common.response.ApiResponse
import com.bandage.bandmanager.global.common.response.CursorResponse
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

@Tag(name = "performances", description = "공연 API")
@RestController
@RequestMapping("$PREFIX/performances")
class PerformanceController(
    private val performanceService: PerformanceService,
    private val performanceSetlistTrackFacade: PerformanceSetlistTrackFacade,
) {
    @PostMapping
    @Operation(operationId = "createPerformance", summary = "공연 생성 API", description = "신규 공연을 생성하고 생성자를 매니저로 등록합니다.")
    fun createPerformance(
        @Valid @RequestBody request: PerformanceCreateRequest,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<PerformanceResponse> = ApiResponse.success(performanceService.createPerformance(request, memberId))

    @GetMapping
    @Operation(
        operationId = "getPerformances",
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
    @Operation(operationId = "getMyPerformances", summary = "내 공연 목록 조회 API", description = "본인이 속한 밴드가 셋리스트로 참여하는 공연 목록을 커서 기반으로 조회합니다.")
    fun getMyPerformances(
        @Valid query: PerformancePagingQuery,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<CursorResponse<PerformanceListResponse, UUID>> =
        ApiResponse.success(performanceService.getMyPerformancesByCursor(memberId, query))

    @GetMapping("/search")
    @Operation(operationId = "searchPerformances", summary = "공연 검색 API", description = "공연 제목에 키워드가 포함된 공연을 커서 기반으로 조회합니다.")
    fun searchPerformances(
        @Valid query: PerformanceSearchQuery,
    ): ApiResponse<CursorResponse<PerformanceListResponse, UUID>> =
        ApiResponse.success(performanceService.searchPerformancesByCursor(query))

    @GetMapping("/{performanceId}")
    @Operation(operationId = "getPerformance", summary = "공연 단건 조회 API", description = "공연 고유 식별 ID를 통해 공연 상세 정보를 조회합니다.")
    fun getPerformance(
        @PathVariable performanceId: UUID,
    ): ApiResponse<PerformanceDetailResponse> = ApiResponse.success(performanceService.getPerformanceDetail(performanceId))

    @PatchMapping("/{performanceId}")
    @Operation(
        operationId = "updatePerformance",
        summary = "공연 정보 수정 API",
        description = "공연 제목, 일정, 장소를 수정합니다. OWNER 또는 MANAGER만 수행할 수 있습니다.",
    )
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
        operationId = "addSetlists",
        summary = "공연 참여 셋리스트 일괄 추가 API",
        description = "공연에 참여 셋리스트를 append 시맨틱으로 다중 추가합니다. OWNER/MANAGER가 본인이 소유/참여한 셋리스트만 추가할 수 있습니다. 이미 등록된 셋리스트는 응답에서 제외.",
    )
    fun addSetlists(
        @PathVariable performanceId: UUID,
        @Valid @RequestBody request: PerformanceSetlistAddRequest,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<List<PerformanceSetlistResponse>> = ApiResponse.success(performanceService.addSetlists(performanceId, request, memberId))

    @GetMapping("/{performanceId}/setlists/tracks")
    @Operation(
        operationId = "getPerformanceSetlistTracks",
        summary = "공연 참여 셋리스트 트랙·참여자 전체 조회 API",
        description =
            "공연에 참여하는 모든 셋리스트와 각 셋리스트의 트랙·참여자 전체를 셋리스트별로 묶어 조회합니다. " +
                "공연 OWNER/MANAGER면 본인이 소유·참여하지 않은 셋리스트의 트랙·참여자도 조회할 수 있습니다. " +
                "참여자 목록은 각 셋리스트 기준(매니저 + 트랙 배정자)이며, 공연에 묶였다는 이유로 확장되지 않습니다.",
    )
    fun getPerformanceSetlistTracks(
        @PathVariable performanceId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<List<PerformanceSetlistTracksResponse>> =
        ApiResponse.success(performanceSetlistTrackFacade.getSetlistTracks(performanceId, memberId))

    @DeleteMapping("/{performanceId}/setlists/{setlistId}")
    @Operation(
        operationId = "removeSetlist",
        summary = "공연 참여 셋리스트 단건 제거 API",
        description = "공연에서 특정 참여 셋리스트를 제거합니다. OWNER는 모든 셋리스트를, MANAGER는 본인이 소유/참여한 셋리스트만 제거할 수 있습니다.",
    )
    fun removeSetlist(
        @PathVariable performanceId: UUID,
        @PathVariable setlistId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        performanceService.removeSetlist(performanceId, setlistId, memberId)
        return ApiResponse.success()
    }

    @PostMapping("/{performanceId}/invitations")
    @Operation(
        operationId = "sendInvitation",
        summary = "공연 매니저 초대 발송 API",
        description = "특정 멤버를 공연 MANAGER로 초대합니다. OWNER만 가능하며, 수락 시 MANAGER로 추가됩니다.",
    )
    fun sendInvitation(
        @PathVariable performanceId: UUID,
        @Valid @RequestBody request: PerformanceInvitationCreateRequest,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<PerformanceInvitationResponse> = ApiResponse.success(performanceService.sendInvitation(performanceId, request, memberId))

    @GetMapping("/{performanceId}/invitations")
    @Operation(
        operationId = "getInvitations",
        summary = "공연 초대 목록 조회 API",
        description = "공연에 발송된 초대 목록을 초대 ID 커서 기반 최신순으로 조회합니다. OWNER만 가능.",
    )
    fun getInvitations(
        @PathVariable performanceId: UUID,
        @Valid query: PerformanceInvitationPagingQuery,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<CursorResponse<PerformanceInvitationResponse, UUID>> =
        ApiResponse.success(performanceService.getInvitations(performanceId, memberId, query))

    @GetMapping("/invitations/me")
    @Operation(
        operationId = "getMyInvitations",
        summary = "내 공연 초대 목록 조회 API",
        description = "본인이 받은 대기 중(PENDING) 공연 초대 목록을 초대 ID 커서 기반 최신순으로 조회합니다.",
    )
    fun getMyInvitations(
        @Valid query: PerformanceInvitationPagingQuery,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<CursorResponse<PerformanceInvitationResponse, UUID>> =
        ApiResponse.success(performanceService.getMyInvitations(memberId, query))

    @PatchMapping("/{performanceId}/invitations/{invitationId}")
    @Operation(
        operationId = "respondInvitation",
        summary = "공연 초대 수락/거절 API",
        description = "받은 공연 초대를 수락(ACCEPTED) 또는 거절(REJECTED)합니다. 초대 수신자 본인만 가능. 수락 시 MANAGER로 추가됩니다.",
    )
    fun respondInvitation(
        @PathVariable performanceId: UUID,
        @PathVariable invitationId: UUID,
        @RequestParam status: PerformanceInvitationStatus,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        performanceService.respondInvitation(performanceId, invitationId, memberId, status)
        return ApiResponse.success()
    }

    @DeleteMapping("/{performanceId}")
    @Operation(operationId = "deletePerformance", summary = "공연 삭제 API", description = "공연을 삭제합니다. OWNER만 수행할 수 있습니다.")
    fun deletePerformance(
        @PathVariable performanceId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        performanceService.deletePerformance(performanceId, memberId)
        return ApiResponse.success()
    }

    @PatchMapping("/{performanceId}/owner")
    @Operation(
        operationId = "delegateOwnership",
        summary = "공연 소유권 양도 API",
        description = "현재 OWNER가 같은 공연의 MANAGER에게 소유권(OWNER)을 양도합니다. 기존 OWNER는 MANAGER로 강등됩니다. OWNER만 수행할 수 있습니다.",
    )
    fun delegateOwnership(
        @PathVariable performanceId: UUID,
        @Valid @RequestBody request: PerformanceOwnerDelegateRequest,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        performanceService.delegateOwnership(performanceId, request.targetMemberId, memberId)
        return ApiResponse.success()
    }
}
