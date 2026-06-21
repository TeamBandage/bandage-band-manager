package com.bandage.bandmanager.domain.band.controller

import com.bandage.bandmanager.domain.band.dto.req.BandCreateRequest
import com.bandage.bandmanager.domain.band.dto.req.BandMemberRoleUpdateRequest
import com.bandage.bandmanager.domain.band.dto.req.BandPagingQuery
import com.bandage.bandmanager.domain.band.dto.req.BandSearchQuery
import com.bandage.bandmanager.domain.band.dto.req.BandUpdateRequest
import com.bandage.bandmanager.domain.band.dto.res.BandInfoResponse
import com.bandage.bandmanager.domain.band.dto.res.BandMemberInfoResponse
import com.bandage.bandmanager.domain.band.dto.res.BandResponse
import com.bandage.bandmanager.domain.band.dto.res.MyBandInfoResponse
import com.bandage.bandmanager.domain.band.service.BandService
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
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "bands", description = "밴드 API")
@RestController
@RequestMapping("$PREFIX/bands")
class BandController(
    private val bandService: BandService,
) {
    @PostMapping
    @Operation(operationId = "createBand", summary = "밴드 생성 API", description = "새로운 밴드를 생성하고 초기 설정을 완료합니다.")
    fun createBand(
        @Valid @RequestBody request: BandCreateRequest,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<BandResponse> =
        ApiResponse.success(
            bandService.createBand(request, memberId),
        )

    @GetMapping("/{bandId}")
    @Operation(operationId = "getBand", summary = "밴드 단건 조회 API", description = "밴드 고유 식별 ID를 통해 밴드 정보를 조회합니다.")
    fun getBand(
        @PathVariable bandId: UUID,
    ): ApiResponse<BandInfoResponse> = ApiResponse.success(bandService.getOnlyOneBand(bandId))

    @GetMapping
    @Operation(operationId = "getBands", summary = "밴드 목록 조회 API", description = "필터 조건에 맞는 밴드 리스트를 페이징하여 조회합니다.")
    fun getBands(
        @Valid query: BandPagingQuery,
    ): ApiResponse<CursorResponse<BandInfoResponse, UUID>> = ApiResponse.success(bandService.getBandsByCursor(query))

    @GetMapping("/me")
    @Operation(
        operationId = "getMyBands",
        summary = "내 밴드 목록 조회 API",
        description = "본인이 소속된 밴드 목록을 커서 기반으로 조회합니다. 응답에 본인의 밴드 내 역할(`myRole`)이 포함됩니다.",
    )
    fun getMyBands(
        @Valid query: BandPagingQuery,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<CursorResponse<MyBandInfoResponse, UUID>> = ApiResponse.success(bandService.getMyBandsByCursor(memberId, query))

    @GetMapping("/search")
    @Operation(operationId = "searchBands", summary = "밴드 검색 API", description = "밴드 이름에 키워드가 포함된 밴드를 커서 기반으로 조회합니다.")
    fun searchBands(
        @Valid query: BandSearchQuery,
    ): ApiResponse<CursorResponse<BandInfoResponse, UUID>> = ApiResponse.success(bandService.searchBandsByCursor(query))

    @GetMapping("/{bandId}/members/{bandMemberId}")
    @Operation(operationId = "getBandMember", summary = "밴드 멤버 단건 조회 API", description = "밴드 내 특정 멤버의 프로필 및 권한 정보를 조회합니다.")
    fun getBandMember(
        @PathVariable bandId: UUID,
        @PathVariable bandMemberId: UUID,
    ): ApiResponse<BandMemberInfoResponse> = ApiResponse.success(bandService.getOnlyOneBandMember(bandMemberId))

    @GetMapping("/{bandId}/members")
    @Operation(operationId = "getBandMembers", summary = "밴드 멤버 목록 조회 API", description = "해당 밴드에 소속된 전체 멤버 목록을 확인합니다.")
    fun getBandMembers(
        @Valid query: BandPagingQuery,
        @PathVariable bandId: UUID,
    ): ApiResponse<CursorResponse<BandMemberInfoResponse, UUID>> = ApiResponse.success(bandService.getBandMembersByCursor(query, bandId))

    @PatchMapping("/{bandId}/members/{bandMemberId}/role")
    @Operation(
        operationId = "delegateLeader",
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
        if (targetRole == null || targetRole == com.bandage.bandmanager.domain.band.model.enums.BandRole.LEADER) {
            bandService.changeLeader(bandId, bandMemberId, memberId)
        } else {
            bandService.changeMemberRole(bandId, bandMemberId, memberId, BandMemberRoleUpdateRequest(targetRole))
        }
        return ApiResponse.success()
    }

    @PatchMapping("/{bandId}")
    @Operation(operationId = "updateBand", summary = "밴드 정보 수정 API", description = "밴드 이름/설명/프로필 이미지 부분 수정. 리더만 가능.")
    fun updateBand(
        @PathVariable bandId: UUID,
        @Valid @RequestBody request: BandUpdateRequest,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<BandResponse> = ApiResponse.success(bandService.updateBand(bandId, request, memberId))

    @DeleteMapping("/{bandId}/profile-image")
    @Operation(
        operationId = "deleteBandProfileImage",
        summary = "밴드 프로필 이미지 삭제 API",
        description = "밴드 프로필 이미지를 제거합니다. 리더만 가능. 이미지가 없어도 성공 처리합니다.",
    )
    fun deleteBandProfileImage(
        @PathVariable bandId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        bandService.deleteProfileImage(bandId, memberId)
        return ApiResponse.success()
    }

    @DeleteMapping("/{bandId}")
    @Operation(operationId = "deleteBand", summary = "밴드 삭제 API", description = "밴드를 소프트 삭제합니다. 리더만 가능.")
    fun deleteBand(
        @PathVariable bandId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        bandService.deleteBand(bandId, memberId)
        return ApiResponse.success()
    }

    @DeleteMapping("/{bandId}/members/{bandMemberId}")
    @Operation(operationId = "kickMember", summary = "밴드 멤버 강퇴 API", description = "리더가 특정 멤버를 강퇴합니다. 리더 자신은 강퇴 불가.")
    fun kickMember(
        @PathVariable bandId: UUID,
        @PathVariable bandMemberId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        bandService.kickMember(bandId, bandMemberId, memberId)
        return ApiResponse.success()
    }

    @DeleteMapping("/{bandId}/members/me")
    @Operation(operationId = "leaveBand", summary = "밴드 탈퇴 API", description = "해당 밴드에서 탈퇴 처리하며 소속 정보를 삭제합니다.")
    fun leaveBand(
        @PathVariable bandId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        bandService.leaveBand(bandId, memberId)
        return ApiResponse.success()
    }
}
