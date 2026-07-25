package com.bandage.bandmanager.domain.band.controller

import com.bandage.bandmanager.domain.band.dto.req.BandApplicationPagingQuery
import com.bandage.bandmanager.domain.band.dto.req.MyBandApplicationPagingQuery
import com.bandage.bandmanager.domain.band.dto.res.BandApplicationInfoResponse
import com.bandage.bandmanager.domain.band.dto.res.MyBandApplicationInfoResponse
import com.bandage.bandmanager.domain.band.model.enums.ApplicationStatus
import com.bandage.bandmanager.domain.band.service.BandService
import com.bandage.bandmanager.global.common.constants.PathPrefix.PREFIX
import com.bandage.bandmanager.global.common.response.ApiResponse
import com.bandage.bandmanager.global.common.response.CursorResponse
import com.bandage.bandmanager.global.security.annotation.CurrentMemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "band-applications", description = "밴드 가입 신청 API")
@RestController
class BandApplicationController(
    private val bandService: BandService,
) {
    @PostMapping("$PREFIX/bands/{bandId}/applications")
    @Operation(operationId = "applyToJoin", summary = "밴드 가입 신청 API", description = "특정 밴드에 가입하기 위해 승인 요청을 보냅니다.")
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

    @GetMapping("$PREFIX/bands/{bandId}/applications")
    @Operation(
        operationId = "getBandApplications",
        summary = "밴드 가입 신청 목록 조회 API",
        description = "필터 조건에 맞는 해당 밴드 가입 요청 목록을 확인합니다. 한 회원당 최신 신청서 1건만 조회됩니다(과거 이력 제외).",
    )
    fun getBandApplications(
        @PathVariable bandId: UUID,
        @Valid query: BandApplicationPagingQuery,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<CursorResponse<BandApplicationInfoResponse, UUID>> =
        ApiResponse.success(bandService.getBandApplicationsByCursor(bandId, query, memberId))

    @GetMapping("$PREFIX/band-applications/me")
    @Operation(
        operationId = "getMyApplications",
        summary = "내 밴드 가입 신청 목록 조회 API",
        description = "본인이 신청한 가입 신청 목록을 밴드와 무관하게 커서 기반으로 조회합니다. `status` 를 지정하면 해당 상태만, 미지정 시 전체 상태를 조회합니다.",
    )
    fun getMyApplications(
        @Valid query: MyBandApplicationPagingQuery,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<CursorResponse<MyBandApplicationInfoResponse, UUID>> =
        ApiResponse.success(bandService.getMyApplicationsByCursor(memberId, query))

    @GetMapping("$PREFIX/bands/{bandId}/applications/me")
    @Operation(
        operationId = "getMyApplicationForBand",
        summary = "내 밴드 가입 신청 단건 조회 API",
        description = "본인이 특정 밴드에 신청한 가장 최근 가입 신청 1건을 상태와 무관하게 조회합니다.",
    )
    fun getMyApplicationForBand(
        @PathVariable bandId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<MyBandApplicationInfoResponse> = ApiResponse.success(bandService.getMyApplicationForBand(bandId, memberId))

    @PatchMapping("$PREFIX/bands/{bandId}/applications/me")
    @Operation(operationId = "withdrawApplication", summary = "밴드 가입 신청 철회 API", description = "승인 대기 중인 본인의 가입 신청을 취소합니다.")
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

    @PatchMapping("$PREFIX/bands/{bandId}/applications/{bandApplicationId}")
    @Operation(operationId = "processApplication", summary = "밴드 가입 신청 승인/거절 API", description = "리더가 특정 신청 건의 상태를 승인 혹은 거절로 변경합니다.")
    fun processApplication(
        @PathVariable bandId: UUID,
        @PathVariable bandApplicationId: UUID,
        @CurrentMemberId memberId: Long,
        @RequestParam status: ApplicationStatus,
    ): ApiResponse<Unit> {
        bandService.processBandApplication(bandId, bandApplicationId, memberId, status)
        return ApiResponse.success()
    }
}
