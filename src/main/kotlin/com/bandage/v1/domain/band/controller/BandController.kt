package com.bandage.v1.domain.band.controller

import com.bandage.v1.domain.band.dto.req.BandApplicationPagingQuery
import com.bandage.v1.domain.band.dto.req.BandCreateRequest
import com.bandage.v1.domain.band.dto.req.BandPagingQuery
import com.bandage.v1.domain.band.dto.res.BandApplicationInfoResponse
import com.bandage.v1.domain.band.dto.res.BandInfoResponse
import com.bandage.v1.domain.band.dto.res.BandResponse
import com.bandage.v1.domain.band.service.BandService
import com.bandage.v1.global.common.constants.PathPrefix.PREFIX
import com.bandage.v1.global.common.response.ApiResponse
import com.bandage.v1.global.common.response.CursorResponse
import com.bandage.v1.global.security.annotation.CurrentMemberId
import com.bandage.v1.global.util.SecurityUtil
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
import java.util.*

@Tag(name = "bands", description = "밴드 API")
@RestController
@RequestMapping("$PREFIX/bands")
class BandController(
    private val bandService: BandService,
) {
    // TODO: (리더) 밴드 가입 신청 승인/거절 API
    // TODO: 밴드 멤버 단일 정보 조회 API
    // TODO: 밴드 멤버 목록 조회 API
    // TODO: 밴드 리더 권한 위임 API
    // TODO: 밴드 탈퇴 API

    @PostMapping
    @Operation(summary = "밴드 생성 API", description = "새로운 밴드를 생성하고 초기 설정을 완료합니다.")
    fun createBand(
        @Valid @RequestBody request: BandCreateRequest,
    ): ApiResponse<BandResponse> =
        ApiResponse.success(
            bandService.createBand(request, SecurityUtil.getCurrentMemberId()),
        )

    @PostMapping("/{bandId}/applications")
    @Operation(summary = "밴드 가입 신청 API", description = "특정 밴드에 가입하기 위해 승인 요청을 보냅니다.")
    fun applyToJoin(
        @PathVariable bandId: UUID,
    ): ApiResponse<Nothing> {
        bandService.createBandApplication(
            bandId = bandId,
            memberId = SecurityUtil.getCurrentMemberId(),
        )
        return ApiResponse.success()
    }

    @PatchMapping("/{bandId}/applications")
    @Operation(summary = "밴드 가입 신청 철회 API", description = "수락 대기 중인 본인의 가입 신청을 취소합니다.")
    fun withdrawApplication(
        @PathVariable bandId: UUID,
    ): ApiResponse<Nothing> {
        bandService.withdrawBandApplication(
            bandId = bandId,
            memberId = SecurityUtil.getCurrentMemberId(),
        )
        return ApiResponse.success()
    }

    @PatchMapping("/{bandId}/applications/{applicationId}")
    @Operation(summary = "밴드 가입 신청 승인/거절 API", description = "리더가 특정 신청 건의 상태를 승인 혹은 거절로 변경합니다.")
    fun processApplication(
        @PathVariable bandId: UUID,
        @PathVariable applicationId: UUID,
    ): ApiResponse<Nothing> = ApiResponse.success()

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

    @GetMapping("/{bandId}/members/{bandMemberId}")
    @Operation(summary = "밴드 멤버 단건 조회 API", description = "밴드 내 특정 멤버의 프로필 및 권한 정보를 조회합니다.")
    fun getBandMember(
        @PathVariable bandId: UUID,
        @PathVariable bandMemberId: UUID,
    ): ApiResponse<Nothing> = ApiResponse.success()

    @GetMapping("/{bandId}/members")
    @Operation(summary = "밴드 멤버 목록 조회 API", description = "해당 밴드에 소속된 전체 멤버 목록을 확인합니다.")
    fun getBandMembers(
        @PathVariable bandId: UUID,
    ): ApiResponse<Nothing> = ApiResponse.success()

    @GetMapping("/{bandId}/applications")
    @Operation(summary = "밴드 가입 신청 목록 조회 API", description = "필터 조건에 맞는 해당 밴드 가입 요청 목록을 확인합니다.")
    fun getBandApplications(
        @PathVariable bandId: UUID,
        @Valid query: BandApplicationPagingQuery,
        @CurrentMemberId memberId: Long
    ): ApiResponse<CursorResponse<BandApplicationInfoResponse, UUID>> =
        ApiResponse.success(bandService.getBandApplicationsByCursor(bandId, query, memberId))

    @PatchMapping("/{bandId}/leader")
    @Operation(summary = "밴드 리더 권한 위임 API", description = "현재 리더가 지정한 멤버에게 리더 권한을 양도합니다.")
    fun delegateLeader(
        @PathVariable bandId: UUID,
    ): ApiResponse<Nothing> = ApiResponse.success()

    @DeleteMapping("/{bandId}/leave")
    @Operation(summary = "밴드 탈퇴 API", description = "해당 밴드에서 탈퇴 처리하며 소속 정보를 삭제합니다.")
    fun leaveBand(
        @PathVariable bandId: UUID,
    ): ApiResponse<Nothing> = ApiResponse.success()
}
