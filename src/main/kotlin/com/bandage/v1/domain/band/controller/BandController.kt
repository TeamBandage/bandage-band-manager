package com.bandage.v1.domain.band.controller

import com.bandage.v1.domain.band.dto.req.BandApplicationPagingQuery
import com.bandage.v1.domain.band.dto.req.BandCreateRequest
import com.bandage.v1.domain.band.dto.req.BandMemberRoleUpdateRequest
import com.bandage.v1.domain.band.dto.req.BandPagingQuery
import com.bandage.v1.domain.band.dto.req.BandSearchQuery
import com.bandage.v1.domain.band.dto.req.BandUpdateRequest
import com.bandage.v1.domain.band.dto.res.BandApplicationInfoResponse
import com.bandage.v1.domain.band.dto.res.BandInfoResponse
import com.bandage.v1.domain.band.dto.res.BandMemberInfoResponse
import com.bandage.v1.domain.band.dto.res.BandResponse
import com.bandage.v1.domain.band.dto.res.MyBandInfoResponse
import com.bandage.v1.domain.band.model.enums.ApplicationStatus
import com.bandage.v1.domain.band.service.BandService
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

@Tag(name = "bands", description = "밴드 API")
@RestController
@RequestMapping("$PREFIX/bands")
class BandController(
    private val bandService: BandService,
) {
    @PostMapping
    @Operation(summary = "밴드 생성 API", description = "새로운 밴드를 생성하고 초기 설정을 완료합니다.")
    fun createBand(
        @Valid @RequestBody request: BandCreateRequest,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<BandResponse> =
        ApiResponse.success(
            bandService.createBand(request, memberId),
        )

    @PostMapping("/{bandId}/applications")
    @Operation(summary = "밴드 가입 신청 API", description = "특정 밴드에 가입하기 위해 승인 요청을 보냅니다.")
    fun applyToJoin(
        @PathVariable bandId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        bandService.createBandApplication(
            bandId = bandId,
            memberId = memberId,
        )
        return ApiResponse.success()
    }

    @GetMapping("/{bandId}")
    @Operation(summary = "밴드 단건 조회 API", description = "밴드 고유 식별 ID를 통해 밴드 정보를 조회합니다.")
    fun getBand(
        @PathVariable bandId: UUID,
    ): ApiResponse<BandInfoResponse> = ApiResponse.success(bandService.getOnlyOneBand(bandId))

    @GetMapping
    @Operation(summary = "밴드 목록 조회 API", description = "필터 조건에 맞는 밴드 리스트를 페이징하여 조회합니다.")
    fun getBands(
        @Valid query: BandPagingQuery,
    ): ApiResponse<CursorResponse<BandInfoResponse, UUID>> = ApiResponse.success(bandService.getBandsByCursor(query))

    @GetMapping("/me")
    @Operation(
        summary = "내 밴드 목록 조회 API",
        description = "본인이 소속된 밴드 목록을 커서 기반으로 조회합니다. 응답에 본인의 밴드 내 역할(`myRole`)이 포함됩니다.",
    )
    fun getMyBands(
        @Valid query: BandPagingQuery,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<CursorResponse<MyBandInfoResponse, UUID>> = ApiResponse.success(bandService.getMyBandsByCursor(memberId, query))

    @GetMapping("/search")
    @Operation(summary = "밴드 검색 API", description = "밴드 이름에 키워드가 포함된 밴드를 커서 기반으로 조회합니다.")
    fun searchBands(
        @Valid query: BandSearchQuery,
    ): ApiResponse<CursorResponse<BandInfoResponse, UUID>> = ApiResponse.success(bandService.searchBandsByCursor(query))

    @GetMapping("/{bandId}/members/{bandMemberId}")
    @Operation(summary = "밴드 멤버 단건 조회 API", description = "밴드 내 특정 멤버의 프로필 및 권한 정보를 조회합니다.")
    fun getBandMember(
        @PathVariable bandId: UUID,
        @PathVariable bandMemberId: UUID,
    ): ApiResponse<BandMemberInfoResponse> = ApiResponse.success(bandService.getOnlyOneBandMember(bandMemberId))

    @GetMapping("/{bandId}/members")
    @Operation(summary = "밴드 멤버 목록 조회 API", description = "해당 밴드에 소속된 전체 멤버 목록을 확인합니다.")
    fun getBandMembers(
        @Valid query: BandPagingQuery,
        @PathVariable bandId: UUID,
    ): ApiResponse<CursorResponse<BandMemberInfoResponse, UUID>> = ApiResponse.success(bandService.getBandMembersByCursor(query, bandId))

    @GetMapping("/{bandId}/applications")
    @Operation(summary = "밴드 가입 신청 목록 조회 API", description = "필터 조건에 맞는 해당 밴드 가입 요청 목록을 확인합니다.")
    fun getBandApplications(
        @PathVariable bandId: UUID,
        @Valid query: BandApplicationPagingQuery,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<CursorResponse<BandApplicationInfoResponse, UUID>> =
        ApiResponse.success(bandService.getBandApplicationsByCursor(bandId, query, memberId))

    @PatchMapping("/{bandId}/applications/me")
    @Operation(summary = "밴드 가입 신청 철회 API", description = "승인 대기 중인 본인의 가입 신청을 취소합니다.")
    fun withdrawApplication(
        @PathVariable bandId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        bandService.withdrawBandApplication(
            bandId = bandId,
            memberId = memberId,
        )
        return ApiResponse.success()
    }

    @PatchMapping("/{bandId}/applications/{bandApplicationId}")
    @Operation(summary = "밴드 가입 신청 승인/거절 API", description = "리더가 특정 신청 건의 상태를 승인 혹은 거절로 변경합니다.")
    fun processApplication(
        @PathVariable bandId: UUID,
        @PathVariable bandApplicationId: UUID,
        @CurrentMemberId memberId: Long,
        @RequestParam status: ApplicationStatus,
    ): ApiResponse<Unit> {
        bandService.processBandApplication(bandId, bandApplicationId, memberId, status)
        return ApiResponse.success()
    }

    @PatchMapping("/{bandId}/members/{bandMemberId}/role")
    @Operation(
        summary = "밴드 멤버 역할 변경 / 리더 위임 API",
        description =
            "리더가 멤버 역할을 변경합니다. body 가 비어 있거나 role=LEADER 면 리더 권한 위임으로 동작하고, " +
                "role=ADMIN/MEMBER 면 해당 역할로 변경합니다.",
    )
    fun delegateLeader(
        @PathVariable bandId: UUID,
        @PathVariable bandMemberId: UUID,
        @CurrentMemberId memberId: Long,
        @RequestBody(required = false) request: BandMemberRoleUpdateRequest?,
    ): ApiResponse<Unit> {
        val targetRole = request?.role
        if (targetRole == null || targetRole == com.bandage.v1.domain.band.model.enums.BandRole.LEADER) {
            bandService.changeLeader(bandId, bandMemberId, memberId)
        } else {
            bandService.changeMemberRole(bandId, bandMemberId, memberId, BandMemberRoleUpdateRequest(targetRole))
        }
        return ApiResponse.success()
    }

    @PatchMapping("/{bandId}")
    @Operation(summary = "밴드 정보 수정 API", description = "밴드 이름/설명/프로필 이미지 부분 수정. 리더만 가능.")
    fun updateBand(
        @PathVariable bandId: UUID,
        @Valid @RequestBody request: BandUpdateRequest,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<BandResponse> = ApiResponse.success(bandService.updateBand(bandId, request, memberId))

    @DeleteMapping("/{bandId}")
    @Operation(summary = "밴드 삭제 API", description = "밴드를 소프트 삭제합니다. 리더만 가능.")
    fun deleteBand(
        @PathVariable bandId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        bandService.deleteBand(bandId, memberId)
        return ApiResponse.success()
    }

    @DeleteMapping("/{bandId}/members/{bandMemberId}")
    @Operation(summary = "밴드 멤버 강퇴 API", description = "리더가 특정 멤버를 강퇴합니다. 리더 자신은 강퇴 불가.")
    fun kickMember(
        @PathVariable bandId: UUID,
        @PathVariable bandMemberId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        bandService.kickMember(bandId, bandMemberId, memberId)
        return ApiResponse.success()
    }

    @DeleteMapping("/{bandId}/members/me")
    @Operation(summary = "밴드 탈퇴 API", description = "해당 밴드에서 탈퇴 처리하며 소속 정보를 삭제합니다.")
    fun leaveBand(
        @PathVariable bandId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        bandService.leaveBand(bandId, memberId)
        return ApiResponse.success()
    }
}
