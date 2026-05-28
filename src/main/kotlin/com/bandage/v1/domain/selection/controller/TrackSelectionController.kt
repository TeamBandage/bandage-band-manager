package com.bandage.v1.domain.selection.controller

import com.bandage.v1.domain.selection.dto.req.SetlistChatMessageCreateRequest
import com.bandage.v1.domain.selection.dto.req.SetlistConfirmationUpdateRequest
import com.bandage.v1.domain.selection.dto.req.SetlistParticipantsUpdateRequest
import com.bandage.v1.domain.selection.dto.req.TrackSelectionCreateRequest
import com.bandage.v1.domain.selection.dto.req.TrackSelectionItemCreateRequest
import com.bandage.v1.domain.selection.dto.req.TrackSelectionItemPagingQuery
import com.bandage.v1.domain.selection.dto.req.TrackSelectionItemSelectionRequest
import com.bandage.v1.domain.selection.dto.req.TrackSelectionItemUpdateRequest
import com.bandage.v1.domain.selection.dto.req.TrackSelectionPagingQuery
import com.bandage.v1.domain.selection.dto.req.TrackSelectionUpdateRequest
import com.bandage.v1.domain.selection.dto.res.SetlistChatMessageResponse
import com.bandage.v1.domain.selection.dto.res.TrackSelectionDetailResponse
import com.bandage.v1.domain.selection.dto.res.TrackSelectionItemResponse
import com.bandage.v1.domain.selection.dto.res.TrackSelectionResponse
import com.bandage.v1.domain.selection.service.TrackSelectionService
import com.bandage.v1.global.common.constants.PathPrefix.PREFIX
import com.bandage.v1.global.common.response.ApiResponse
import com.bandage.v1.global.common.response.CursorResponse
import com.bandage.v1.global.security.annotation.CurrentMemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
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

@Tag(name = "track-selections", description = "선곡 API")
@RestController
@RequestMapping("$PREFIX/track-selections")
class TrackSelectionController(
    private val trackSelectionService: TrackSelectionService,
) {
    @PostMapping
    @Operation(summary = "선곡 생성", description = "선곡을 생성합니다.")
    fun createSelection(
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: TrackSelectionCreateRequest,
    ): ApiResponse<TrackSelectionResponse> = ApiResponse.success(trackSelectionService.createSelection(memberId, request))

    @GetMapping("/me")
    @Operation(summary = "내 선곡 목록", description = "본인이 참여 중인 선곡을 커서 기반으로 조회합니다.")
    fun getMySelections(
        @CurrentMemberId memberId: Long,
        @Valid query: TrackSelectionPagingQuery,
    ): ApiResponse<CursorResponse<TrackSelectionResponse, UUID>> =
        ApiResponse.success(trackSelectionService.getMySelections(memberId, query))

    @GetMapping("/{selectionId}")
    @Operation(summary = "선곡 단건 조회")
    fun getSelection(
        @PathVariable selectionId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<TrackSelectionDetailResponse> = ApiResponse.success(trackSelectionService.getSelection(selectionId, memberId))

    @PatchMapping("/{selectionId}")
    @Operation(summary = "선곡 수정")
    fun updateSelection(
        @PathVariable selectionId: UUID,
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: TrackSelectionUpdateRequest,
    ): ApiResponse<TrackSelectionResponse> = ApiResponse.success(trackSelectionService.updateSelection(selectionId, memberId, request))

    @DeleteMapping("/{selectionId}")
    @Operation(summary = "선곡 삭제")
    fun deleteSelection(
        @PathVariable selectionId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        trackSelectionService.deleteSelection(selectionId, memberId)
        return ApiResponse.success()
    }

    @PatchMapping("/{selectionId}/participants")
    @Operation(summary = "선곡 참여자 변경", description = "매니저가 참여자를 추가/제거합니다. remove 멤버의 세션 지원/확정은 cascade 정리됩니다.")
    fun updateParticipants(
        @PathVariable selectionId: UUID,
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: SetlistParticipantsUpdateRequest,
    ): ApiResponse<TrackSelectionDetailResponse> =
        ApiResponse.success(trackSelectionService.updateParticipants(selectionId, memberId, request))

    // -------- items --------
    @GetMapping("/{selectionId}/items")
    @Operation(summary = "선곡 항목 목록 조회")
    fun getItems(
        @PathVariable selectionId: UUID,
        @CurrentMemberId memberId: Long,
        @Valid query: TrackSelectionItemPagingQuery,
    ): ApiResponse<CursorResponse<TrackSelectionItemResponse, UUID>> =
        ApiResponse.success(trackSelectionService.getItems(selectionId, memberId, query))

    @GetMapping("/{selectionId}/items/{itemId}")
    @Operation(summary = "선곡 항목 단건 조회")
    fun getItem(
        @PathVariable selectionId: UUID,
        @PathVariable itemId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<TrackSelectionItemResponse> = ApiResponse.success(trackSelectionService.getItem(selectionId, itemId, memberId))

    @PostMapping("/{selectionId}/items")
    @Operation(summary = "선곡 항목 생성")
    fun createItem(
        @PathVariable selectionId: UUID,
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: TrackSelectionItemCreateRequest,
    ): ApiResponse<TrackSelectionItemResponse> = ApiResponse.success(trackSelectionService.createItem(selectionId, memberId, request))

    @PatchMapping("/{selectionId}/items/{itemId}")
    @Operation(summary = "선곡 항목 수정")
    fun updateItem(
        @PathVariable selectionId: UUID,
        @PathVariable itemId: UUID,
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: TrackSelectionItemUpdateRequest,
    ): ApiResponse<TrackSelectionItemResponse> =
        ApiResponse.success(trackSelectionService.updateItem(selectionId, itemId, memberId, request))

    @DeleteMapping("/{selectionId}/items/{itemId}")
    @Operation(summary = "선곡 항목 삭제")
    fun deleteItem(
        @PathVariable selectionId: UUID,
        @PathVariable itemId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        trackSelectionService.deleteItem(selectionId, itemId, memberId)
        return ApiResponse.success()
    }

    @PatchMapping("/{selectionId}/items/{itemId}/selection")
    @Operation(
        summary = "선곡 항목 선택/해제",
        description = "매니저가 항목의 선택 상태를 토글합니다. true 전환 시 모든 세션의 확정 인원이 충족되어야 합니다.",
    )
    fun updateItemSelection(
        @PathVariable selectionId: UUID,
        @PathVariable itemId: UUID,
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: TrackSelectionItemSelectionRequest,
    ): ApiResponse<TrackSelectionItemResponse> =
        ApiResponse.success(trackSelectionService.updateItemSelection(selectionId, itemId, memberId, request))

    // -------- session applicants --------
    @PostMapping("/{selectionId}/items/{itemId}/sessions/{sessionId}/applicants")
    @Operation(summary = "세션 지원", description = "본인을 세션 지원자로 등록합니다.")
    fun applyForSession(
        @PathVariable selectionId: UUID,
        @PathVariable itemId: UUID,
        @PathVariable sessionId: String,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        trackSelectionService.applyForSession(selectionId, itemId, sessionId, memberId)
        return ApiResponse.success()
    }

    @DeleteMapping("/{selectionId}/items/{itemId}/sessions/{sessionId}/applicants/{userId}")
    @Operation(summary = "세션 지원 철회", description = "본인의 세션 지원을 철회합니다.")
    fun withdrawSessionApplication(
        @PathVariable selectionId: UUID,
        @PathVariable itemId: UUID,
        @PathVariable sessionId: String,
        @PathVariable userId: Long,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        trackSelectionService.withdrawSessionApplication(selectionId, itemId, sessionId, userId, memberId)
        return ApiResponse.success()
    }

    @PatchMapping("/{selectionId}/items/{itemId}/sessions/{sessionId}/confirmations")
    @Operation(summary = "세션 확정/해제", description = "매니저가 세션 참여자를 확정/해제 합니다.")
    fun updateConfirmations(
        @PathVariable selectionId: UUID,
        @PathVariable itemId: UUID,
        @PathVariable sessionId: String,
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: SetlistConfirmationUpdateRequest,
    ): ApiResponse<TrackSelectionItemResponse> =
        ApiResponse.success(trackSelectionService.updateConfirmations(selectionId, itemId, sessionId, memberId, request))

    // -------- chat --------
    @GetMapping("/{selectionId}/items/{itemId}/chat")
    @Operation(summary = "선곡 항목 채팅 조회")
    fun getChatMessages(
        @PathVariable selectionId: UUID,
        @PathVariable itemId: UUID,
        @CurrentMemberId memberId: Long,
        @RequestParam(required = false) lastId: UUID?,
        @RequestParam(defaultValue = "50") @Min(1) @Max(200) pageSize: Int,
    ): ApiResponse<CursorResponse<SetlistChatMessageResponse, UUID>> =
        ApiResponse.success(trackSelectionService.getChatMessages(selectionId, itemId, memberId, lastId, pageSize))

    @PostMapping("/{selectionId}/items/{itemId}/chat")
    @Operation(summary = "선곡 항목 채팅 작성")
    fun createChatMessage(
        @PathVariable selectionId: UUID,
        @PathVariable itemId: UUID,
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: SetlistChatMessageCreateRequest,
    ): ApiResponse<SetlistChatMessageResponse> =
        ApiResponse.success(trackSelectionService.createChatMessage(selectionId, itemId, memberId, request))

    // -------- lock / unlock --------
    @PostMapping("/{selectionId}/lock")
    @Operation(
        summary = "선곡 잠금",
        description = "매니저가 선곡을 잠금 처리합니다.",
    )
    fun lockSelection(
        @PathVariable selectionId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<TrackSelectionResponse> = ApiResponse.success(trackSelectionService.lockSelection(selectionId, memberId))

    @PostMapping("/{selectionId}/unlock")
    @Operation(summary = "선곡 잠금 해제")
    fun unlockSelection(
        @PathVariable selectionId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<TrackSelectionResponse> = ApiResponse.success(trackSelectionService.unlockSelection(selectionId, memberId))
}
