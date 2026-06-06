package com.bandage.v1.domain.jam.controller

import com.bandage.v1.domain.jam.dto.req.JamCreateRequest
import com.bandage.v1.domain.jam.dto.req.JamMemberAddRequest
import com.bandage.v1.domain.jam.dto.req.JamPagingQuery
import com.bandage.v1.domain.jam.dto.req.JamSearchQuery
import com.bandage.v1.domain.jam.dto.req.JamSessionsUpdateRequest
import com.bandage.v1.domain.jam.dto.req.JamTimeInfoUpdateRequest
import com.bandage.v1.domain.jam.dto.req.JamVenueUpdateRequest
import com.bandage.v1.domain.jam.dto.res.JamDetailResponse
import com.bandage.v1.domain.jam.dto.res.JamListResponse
import com.bandage.v1.domain.jam.dto.res.JamParticipantResponse
import com.bandage.v1.domain.jam.dto.res.JamResponse
import com.bandage.v1.domain.jam.service.JamService
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
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "jams", description = "합주 API")
@RestController
@RequestMapping("$PREFIX/jams")
class JamController(
    private val jamService: JamService,
) {
    @PostMapping
    @Operation(operationId = "createJam", summary = "합주 생성 API", description = "신규 합주를 생성합니다.")
    fun createJam(
        @Valid @RequestBody request: JamCreateRequest,
    ): ApiResponse<JamResponse> =
        ApiResponse.success(
            jamService.createJam(request),
        )

    @GetMapping
    @Operation(
        operationId = "getJams",
        summary = "합주 목록 조회 API",
        description = "합주 목록을 커서 기반으로 조회합니다. bandId 제공 시 해당 밴드 멤버가 참여 중인 합주만 조회합니다.",
    )
    fun getJams(
        @RequestParam(required = false) bandId: UUID?,
        @Valid query: JamPagingQuery,
    ): ApiResponse<CursorResponse<JamListResponse, UUID>> = ApiResponse.success(jamService.getJamsByCursor(bandId, query))

    @GetMapping("/{jamId}")
    @Operation(operationId = "getJam", summary = "합주 조회 API", description = "합주 상세 정보를 조회합니다.")
    fun getJam(
        @PathVariable jamId: UUID,
    ): ApiResponse<JamDetailResponse> =
        ApiResponse.success(
            jamService.getJamDetail(jamId),
        )

    @GetMapping("/me")
    @Operation(operationId = "getMyJams", summary = "내 합주 목록 조회 API", description = "본인이 참여 중인 합주 목록을 커서 기반으로 조회합니다.")
    fun getMyJams(
        @Valid query: JamPagingQuery,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<CursorResponse<JamListResponse, UUID>> = ApiResponse.success(jamService.getMyJamsByCursor(memberId, query))

    @GetMapping("/me/search")
    @Operation(
        operationId = "searchMyJams",
        summary = "내 합주 검색 API",
        description = "본인이 참여 중인 합주 중 합주 타이틀 또는 곡 제목에 키워드가 포함된 합주를 커서 기반으로 조회합니다.",
    )
    fun searchMyJams(
        @Valid query: JamSearchQuery,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<CursorResponse<JamListResponse, UUID>> = ApiResponse.success(jamService.searchMyJamsByCursor(memberId, query))

    @PutMapping("/{jamId}/sessions")
    @Operation(
        operationId = "updateSessions",
        summary = "합주 세션 정의 교체 API",
        description = "합주의 세션 정의 목록을 전체 교체합니다. 제거된 세션의 참여자 배정은 함께 삭제됩니다.",
    )
    fun updateSessions(
        @PathVariable jamId: UUID,
        @Valid @RequestBody request: JamSessionsUpdateRequest,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<JamDetailResponse> =
        ApiResponse.success(
            jamService.updateSessions(jamId, request),
        )

    @PostMapping("/{jamId}/participants")
    @Operation(operationId = "addParticipant", summary = "합주 세션 참여자 추가 API", description = "합주의 특정 세션에 멤버를 배정합니다.")
    fun addParticipant(
        @PathVariable jamId: UUID,
        @Valid @RequestBody request: JamMemberAddRequest,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<JamParticipantResponse> =
        ApiResponse.success(
            jamService.addParticipant(jamId, request),
        )

    @DeleteMapping("/{jamId}/participants/{participantId}")
    @Operation(operationId = "deleteParticipant", summary = "합주 세션 참여자 삭제 API", description = "합주 세션 참여자 배정을 삭제합니다.")
    fun deleteParticipant(
        @PathVariable jamId: UUID,
        @PathVariable participantId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        jamService.deleteParticipant(jamId, participantId, memberId)
        return ApiResponse.success()
    }

    @PatchMapping("/{jamId}/time-info")
    @Operation(operationId = "updateTimeInfo", summary = "합주 일정 변경 API", description = "합주 일정(시작 시간, 소요 시간)을 변경합니다.")
    fun updateTimeInfo(
        @PathVariable jamId: UUID,
        @Valid @RequestBody request: JamTimeInfoUpdateRequest,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        jamService.updateTimeInfo(jamId, request)
        return ApiResponse.success()
    }

    @PatchMapping("/{jamId}/venue")
    @Operation(operationId = "updateVenue", summary = "합주 장소 변경 API", description = "합주 장소를 변경합니다.")
    fun updateVenue(
        @PathVariable jamId: UUID,
        @Valid @RequestBody request: JamVenueUpdateRequest,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        jamService.updateVenue(jamId, request)
        return ApiResponse.success()
    }

    @DeleteMapping("/{jamId}")
    @Operation(operationId = "deleteJam", summary = "합주 삭제 API", description = "합주를 삭제합니다.")
    fun deleteJam(
        @PathVariable jamId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        jamService.deleteJam(jamId, memberId)
        return ApiResponse.success()
    }
}
