package com.bandage.v1.domain.practice.controller

import com.bandage.v1.domain.practice.dto.req.PracticeCreateRequest
import com.bandage.v1.domain.practice.dto.req.PracticeMemberAddRequest
import com.bandage.v1.domain.practice.dto.req.PracticePagingQuery
import com.bandage.v1.domain.practice.dto.req.PracticeScheduleUpdateRequest
import com.bandage.v1.domain.practice.dto.req.PracticeSearchQuery
import com.bandage.v1.domain.practice.dto.req.PracticeSessionCreateRequest
import com.bandage.v1.domain.practice.dto.req.PracticeVenueUpdateRequest
import com.bandage.v1.domain.practice.dto.res.PracticeDetailResponse
import com.bandage.v1.domain.practice.dto.res.PracticeListResponse
import com.bandage.v1.domain.practice.dto.res.PracticeParticipantResponse
import com.bandage.v1.domain.practice.dto.res.PracticeResponse
import com.bandage.v1.domain.practice.dto.res.PracticeSessionResponse
import com.bandage.v1.domain.practice.service.PracticeService
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

@Tag(name = "practices", description = "합주 API")
@RestController
@RequestMapping("$PREFIX/practices")
class PracticeController(
    private val practiceService: PracticeService,
) {
    @PostMapping
    @Operation(summary = "합주 생성 API", description = "신규 합주를 생성합니다.")
    fun createPractice(
        @Valid @RequestBody request: PracticeCreateRequest,
    ): ApiResponse<PracticeResponse> =
        ApiResponse.success(
            practiceService.createPractice(request),
        )

    @GetMapping
    @Operation(
        summary = "합주 목록 조회 API",
        description = "합주 목록을 커서 기반으로 조회합니다. bandId 제공 시 해당 밴드 멤버가 참여 중인 합주만 조회합니다.",
    )
    fun getPractices(
        @RequestParam(required = false) bandId: UUID?,
        @Valid query: PracticePagingQuery,
    ): ApiResponse<CursorResponse<PracticeListResponse, UUID>> = ApiResponse.success(practiceService.getPracticesByCursor(bandId, query))

    @GetMapping("/{practiceId}")
    @Operation(summary = "합주 조회 API", description = "합주 상세 정보를 조회합니다.")
    fun getPractice(
        @PathVariable practiceId: UUID,
    ): ApiResponse<PracticeDetailResponse> =
        ApiResponse.success(
            practiceService.getPracticeDetail(practiceId),
        )

    @GetMapping("/me")
    @Operation(summary = "내 합주 목록 조회 API", description = "본인이 참여 중인 합주 목록을 커서 기반으로 조회합니다.")
    fun getMyPractices(
        @Valid query: PracticePagingQuery,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<CursorResponse<PracticeListResponse, UUID>> =
        ApiResponse.success(practiceService.getMyPracticesByCursor(memberId, query))

    @GetMapping("/me/search")
    @Operation(summary = "내 합주 검색 API", description = "본인이 참여 중인 합주 중 합주 타이틀 또는 곡 제목에 키워드가 포함된 합주를 커서 기반으로 조회합니다.")
    fun searchMyPractices(
        @Valid query: PracticeSearchQuery,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<CursorResponse<PracticeListResponse, UUID>> =
        ApiResponse.success(practiceService.searchMyPracticesByCursor(memberId, query))

    @PostMapping("/{practiceId}/sessions")
    @Operation(summary = "합주 세션 생성 API", description = "합주에 세션을 추가합니다.")
    fun createSession(
        @PathVariable practiceId: UUID,
        @Valid @RequestBody request: PracticeSessionCreateRequest,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<PracticeSessionResponse> =
        ApiResponse.success(
            practiceService.createSession(practiceId, request),
        )

    @DeleteMapping("/{practiceId}/sessions/{sessionId}")
    @Operation(summary = "합주 세션 삭제 API", description = "합주에서 세션을 삭제합니다.")
    fun deleteSession(
        @PathVariable practiceId: UUID,
        @PathVariable sessionId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        practiceService.deleteSession(practiceId, sessionId, memberId)
        return ApiResponse.success()
    }

    @PostMapping("/{practiceId}/participants")
    @Operation(summary = "합주 멤버 추가 API", description = "합주에 멤버를 추가합니다.")
    fun addParticipant(
        @PathVariable practiceId: UUID,
        @Valid @RequestBody request: PracticeMemberAddRequest,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<PracticeParticipantResponse> =
        ApiResponse.success(
            practiceService.addParticipant(practiceId, request),
        )

    @PatchMapping("/{practiceId}/sessions/{sessionId}/assignment")
    @Operation(summary = "합주 세션 멤버 지정 API", description = "본인을 합주 세션에 배정합니다.")
    fun assignSessionParticipant(
        @PathVariable practiceId: UUID,
        @PathVariable sessionId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        practiceService.assignSessionParticipant(practiceId, sessionId, memberId)
        return ApiResponse.success()
    }

    @DeleteMapping("/{practiceId}/sessions/{sessionId}/assignment")
    @Operation(summary = "합주 세션 멤버 지정 취소 API", description = "본인의 합주 세션 배정을 취소합니다.")
    fun withdrawSessionParticipant(
        @PathVariable practiceId: UUID,
        @PathVariable sessionId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        practiceService.withdrawSessionParticipant(practiceId, sessionId, memberId)
        return ApiResponse.success()
    }

    @PatchMapping("/{practiceId}/schedule")
    @Operation(summary = "합주 일정 변경 API", description = "합주 일정(시작 시간, 소요 시간)을 변경합니다.")
    fun updateSchedule(
        @PathVariable practiceId: UUID,
        @Valid @RequestBody request: PracticeScheduleUpdateRequest,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        practiceService.updateSchedule(practiceId, request)
        return ApiResponse.success()
    }

    @PatchMapping("/{practiceId}/venue")
    @Operation(summary = "합주 장소 변경 API", description = "합주 장소를 변경합니다.")
    fun updateVenue(
        @PathVariable practiceId: UUID,
        @Valid @RequestBody request: PracticeVenueUpdateRequest,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        practiceService.updateVenue(practiceId, request)
        return ApiResponse.success()
    }

    @DeleteMapping("/{practiceId}")
    @Operation(summary = "합주 삭제 API", description = "합주를 삭제합니다.")
    fun deletePractice(
        @PathVariable practiceId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        practiceService.deletePractice(practiceId, memberId)
        return ApiResponse.success()
    }
}
