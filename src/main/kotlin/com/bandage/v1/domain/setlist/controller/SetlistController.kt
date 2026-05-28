package com.bandage.v1.domain.setlist.controller

import com.bandage.v1.domain.setlist.dto.req.SetlistCreateRequest
import com.bandage.v1.domain.setlist.dto.req.SetlistPagingQuery
import com.bandage.v1.domain.setlist.dto.req.SetlistTrackPagingQuery
import com.bandage.v1.domain.setlist.dto.req.SetlistTrackUpdateRequest
import com.bandage.v1.domain.setlist.dto.req.SetlistUpdateRequest
import com.bandage.v1.domain.setlist.dto.res.SetlistDetailResponse
import com.bandage.v1.domain.setlist.dto.res.SetlistResponse
import com.bandage.v1.domain.setlist.dto.res.SetlistTrackResponse
import com.bandage.v1.domain.setlist.service.SetlistService
import com.bandage.v1.facade.SetlistCreateFacade
import com.bandage.v1.global.common.constants.PathPrefix.PREFIX
import com.bandage.v1.global.common.response.ApiResponse
import com.bandage.v1.global.common.response.CursorResponse
import com.bandage.v1.global.security.annotation.CurrentMemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
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

@Tag(name = "setlists", description = "셋리스트 API")
@RestController
@RequestMapping("$PREFIX/setlists")
class SetlistController(
    private val setlistCreateFacade: SetlistCreateFacade,
    private val setlistService: SetlistService,
) {
    @PostMapping
    @Operation(summary = "셋리스트 생성", description = "잠금된 선곡(TrackSelection)에서 선택된 트랙들을 모아 셋리스트를 생성합니다.")
    fun createSetlist(
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: SetlistCreateRequest,
    ): ApiResponse<SetlistResponse> = ApiResponse.success(setlistCreateFacade.createSetlist(memberId, request))

    @GetMapping("/me")
    @Operation(summary = "내 셋리스트 목록", description = "본인이 참여 중인 셋리스트를 커서 기반으로 조회합니다.")
    fun getMySetlists(
        @CurrentMemberId memberId: Long,
        @Valid query: SetlistPagingQuery,
    ): ApiResponse<CursorResponse<SetlistResponse, UUID>> = ApiResponse.success(setlistService.getMySetlists(memberId, query))

    @GetMapping("/by-title")
    @Operation(summary = "셋리스트 타이틀 조회", description = "본인이 접근 가능한 셋리스트 중 타이틀이 일치하는 항목을 조회합니다.")
    fun getSetlistsByTitle(
        @CurrentMemberId memberId: Long,
        @RequestParam @NotBlank title: String,
    ): ApiResponse<List<SetlistResponse>> = ApiResponse.success(setlistService.getSetlistsByTitle(title, memberId))

    @GetMapping("/{setlistId}")
    @Operation(summary = "셋리스트 단건 조회")
    fun getSetlist(
        @PathVariable setlistId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<SetlistDetailResponse> = ApiResponse.success(setlistService.getSetlist(setlistId, memberId))

    @PatchMapping("/{setlistId}")
    @Operation(summary = "셋리스트 타이틀 수정", description = "매니저가 셋리스트 제목을 수정합니다.")
    fun updateSetlist(
        @PathVariable setlistId: UUID,
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: SetlistUpdateRequest,
    ): ApiResponse<SetlistDetailResponse> = ApiResponse.success(setlistService.updateSetlist(setlistId, memberId, request))

    // -------- tracks --------
    @GetMapping("/{setlistId}/tracks")
    @Operation(summary = "셋리스트 트랙 목록 조회")
    fun getTracks(
        @PathVariable setlistId: UUID,
        @CurrentMemberId memberId: Long,
        @Valid query: SetlistTrackPagingQuery,
    ): ApiResponse<CursorResponse<SetlistTrackResponse, UUID>> = ApiResponse.success(setlistService.getTracks(setlistId, memberId, query))

    @GetMapping("/{setlistId}/tracks/{trackId}")
    @Operation(summary = "셋리스트 트랙 단건 조회")
    fun getTrack(
        @PathVariable setlistId: UUID,
        @PathVariable trackId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<SetlistTrackResponse> = ApiResponse.success(setlistService.getTrack(setlistId, trackId, memberId))

    @PatchMapping("/{setlistId}/tracks/{trackId}")
    @Operation(
        summary = "셋리스트 트랙 수정",
        description = "매니저가 셋리스트 트랙을 수정합니다. 세션 교체 시 더 이상 참여하지 않는 참여자도 함께 정리됩니다.",
    )
    fun updateTrack(
        @PathVariable setlistId: UUID,
        @PathVariable trackId: UUID,
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: SetlistTrackUpdateRequest,
    ): ApiResponse<SetlistTrackResponse> = ApiResponse.success(setlistService.updateTrack(setlistId, trackId, memberId, request))

    @DeleteMapping("/{setlistId}/tracks/{trackId}")
    @Operation(summary = "셋리스트 트랙 삭제", description = "매니저가 셋리스트 트랙을 삭제합니다. 해당 트랙의 참여자도 함께 정리됩니다.")
    fun deleteTrack(
        @PathVariable setlistId: UUID,
        @PathVariable trackId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        setlistService.deleteTrack(setlistId, trackId, memberId)
        return ApiResponse.success()
    }
}
