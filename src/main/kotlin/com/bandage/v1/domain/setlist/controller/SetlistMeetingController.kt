package com.bandage.v1.domain.setlist.controller

import com.bandage.v1.domain.setlist.dto.req.SetlistChatMessageCreateRequest
import com.bandage.v1.domain.setlist.dto.req.SetlistConfirmationUpdateRequest
import com.bandage.v1.domain.setlist.dto.req.SetlistItemCreateRequest
import com.bandage.v1.domain.setlist.dto.req.SetlistItemPagingQuery
import com.bandage.v1.domain.setlist.dto.req.SetlistItemUpdateRequest
import com.bandage.v1.domain.setlist.dto.req.SetlistMeetingCreateRequest
import com.bandage.v1.domain.setlist.dto.req.SetlistMeetingPagingQuery
import com.bandage.v1.domain.setlist.dto.req.SetlistMeetingUpdateRequest
import com.bandage.v1.domain.setlist.dto.req.SetlistParticipantsUpdateRequest
import com.bandage.v1.domain.setlist.dto.res.SetlistChatMessageResponse
import com.bandage.v1.domain.setlist.dto.res.SetlistItemResponse
import com.bandage.v1.domain.setlist.dto.res.SetlistLockResponse
import com.bandage.v1.domain.setlist.dto.res.SetlistMeetingDetailResponse
import com.bandage.v1.domain.setlist.dto.res.SetlistMeetingResponse
import com.bandage.v1.domain.setlist.service.SetlistMeetingService
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

@Tag(name = "setlist-meetings", description = "선곡 회의 API")
@RestController
@RequestMapping("$PREFIX/setlist-meetings")
class SetlistMeetingController(
    private val setlistMeetingService: SetlistMeetingService,
) {
    @PostMapping
    @Operation(summary = "선곡 회의 생성", description = "선곡 회의를 생성합니다.")
    fun createMeeting(
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: SetlistMeetingCreateRequest,
    ): ApiResponse<SetlistMeetingResponse> = ApiResponse.success(setlistMeetingService.createMeeting(memberId, request))

    @GetMapping("/me")
    @Operation(summary = "내 선곡 회의 목록", description = "본인이 참여 중인 선곡 회의를 커서 기반으로 조회합니다.")
    fun getMyMeetings(
        @CurrentMemberId memberId: Long,
        @Valid query: SetlistMeetingPagingQuery,
    ): ApiResponse<CursorResponse<SetlistMeetingResponse, UUID>> = ApiResponse.success(setlistMeetingService.getMyMeetings(memberId, query))

    @GetMapping("/{meetingId}")
    @Operation(summary = "선곡 회의 단건 조회")
    fun getMeeting(
        @PathVariable meetingId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<SetlistMeetingDetailResponse> = ApiResponse.success(setlistMeetingService.getMeeting(meetingId, memberId))

    @PatchMapping("/{meetingId}")
    @Operation(summary = "선곡 회의 수정")
    fun updateMeeting(
        @PathVariable meetingId: UUID,
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: SetlistMeetingUpdateRequest,
    ): ApiResponse<SetlistMeetingResponse> = ApiResponse.success(setlistMeetingService.updateMeeting(meetingId, memberId, request))

    @DeleteMapping("/{meetingId}")
    @Operation(summary = "선곡 회의 삭제")
    fun deleteMeeting(
        @PathVariable meetingId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        setlistMeetingService.deleteMeeting(meetingId, memberId)
        return ApiResponse.success()
    }

    @PatchMapping("/{meetingId}/participants")
    @Operation(summary = "선곡 회의 참여자 변경", description = "매니저가 참여자를 추가/제거합니다. remove 멤버의 세션 지원/확정은 cascade 정리됩니다.")
    fun updateParticipants(
        @PathVariable meetingId: UUID,
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: SetlistParticipantsUpdateRequest,
    ): ApiResponse<SetlistMeetingDetailResponse> =
        ApiResponse.success(setlistMeetingService.updateParticipants(meetingId, memberId, request))

    // -------- items --------
    @GetMapping("/{meetingId}/items")
    @Operation(summary = "선곡 항목 목록 조회")
    fun getItems(
        @PathVariable meetingId: UUID,
        @CurrentMemberId memberId: Long,
        @Valid query: SetlistItemPagingQuery,
    ): ApiResponse<CursorResponse<SetlistItemResponse, UUID>> =
        ApiResponse.success(setlistMeetingService.getItems(meetingId, memberId, query))

    @GetMapping("/{meetingId}/items/{itemId}")
    @Operation(summary = "선곡 항목 단건 조회")
    fun getItem(
        @PathVariable meetingId: UUID,
        @PathVariable itemId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<SetlistItemResponse> = ApiResponse.success(setlistMeetingService.getItem(meetingId, itemId, memberId))

    @PostMapping("/{meetingId}/items")
    @Operation(summary = "선곡 항목 생성")
    fun createItem(
        @PathVariable meetingId: UUID,
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: SetlistItemCreateRequest,
    ): ApiResponse<SetlistItemResponse> = ApiResponse.success(setlistMeetingService.createItem(meetingId, memberId, request))

    @PatchMapping("/{meetingId}/items/{itemId}")
    @Operation(summary = "선곡 항목 수정")
    fun updateItem(
        @PathVariable meetingId: UUID,
        @PathVariable itemId: UUID,
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: SetlistItemUpdateRequest,
    ): ApiResponse<SetlistItemResponse> = ApiResponse.success(setlistMeetingService.updateItem(meetingId, itemId, memberId, request))

    @DeleteMapping("/{meetingId}/items/{itemId}")
    @Operation(summary = "선곡 항목 삭제")
    fun deleteItem(
        @PathVariable meetingId: UUID,
        @PathVariable itemId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        setlistMeetingService.deleteItem(meetingId, itemId, memberId)
        return ApiResponse.success()
    }

    // -------- session applicants --------
    @PostMapping("/{meetingId}/items/{itemId}/sessions/{sessionId}/applicants")
    @Operation(summary = "세션 지원", description = "본인을 세션 지원자로 등록합니다.")
    fun applyForSession(
        @PathVariable meetingId: UUID,
        @PathVariable itemId: UUID,
        @PathVariable sessionId: String,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        setlistMeetingService.applyForSession(meetingId, itemId, sessionId, memberId)
        return ApiResponse.success()
    }

    @DeleteMapping("/{meetingId}/items/{itemId}/sessions/{sessionId}/applicants/{userId}")
    @Operation(summary = "세션 지원 철회", description = "본인의 세션 지원을 철회합니다.")
    fun withdrawSessionApplication(
        @PathVariable meetingId: UUID,
        @PathVariable itemId: UUID,
        @PathVariable sessionId: String,
        @PathVariable userId: Long,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        setlistMeetingService.withdrawSessionApplication(meetingId, itemId, sessionId, userId, memberId)
        return ApiResponse.success()
    }

    @PatchMapping("/{meetingId}/items/{itemId}/sessions/{sessionId}/confirmations")
    @Operation(summary = "세션 확정/해제", description = "매니저가 세션 참여자를 확정/해제 합니다.")
    fun updateConfirmations(
        @PathVariable meetingId: UUID,
        @PathVariable itemId: UUID,
        @PathVariable sessionId: String,
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: SetlistConfirmationUpdateRequest,
    ): ApiResponse<SetlistItemResponse> =
        ApiResponse.success(setlistMeetingService.updateConfirmations(meetingId, itemId, sessionId, memberId, request))

    // -------- chat --------
    @GetMapping("/{meetingId}/items/{itemId}/chat")
    @Operation(summary = "선곡 항목 채팅 조회")
    fun getChatMessages(
        @PathVariable meetingId: UUID,
        @PathVariable itemId: UUID,
        @CurrentMemberId memberId: Long,
        @RequestParam(required = false) lastId: UUID?,
        @RequestParam(defaultValue = "50") @Min(1) @Max(200) pageSize: Int,
    ): ApiResponse<CursorResponse<SetlistChatMessageResponse, UUID>> =
        ApiResponse.success(setlistMeetingService.getChatMessages(meetingId, itemId, memberId, lastId, pageSize))

    @PostMapping("/{meetingId}/items/{itemId}/chat")
    @Operation(summary = "선곡 항목 채팅 작성")
    fun createChatMessage(
        @PathVariable meetingId: UUID,
        @PathVariable itemId: UUID,
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: SetlistChatMessageCreateRequest,
    ): ApiResponse<SetlistChatMessageResponse> =
        ApiResponse.success(setlistMeetingService.createChatMessage(meetingId, itemId, memberId, request))

    // -------- lock / unlock --------
    @PostMapping("/{meetingId}/lock")
    @Operation(summary = "선곡 회의 잠금", description = "매니저가 선곡 회의를 잠금 처리합니다.")
    fun lockMeeting(
        @PathVariable meetingId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<SetlistLockResponse> = ApiResponse.success(setlistMeetingService.lockMeeting(meetingId, memberId))

    @PostMapping("/{meetingId}/unlock")
    @Operation(summary = "선곡 회의 잠금 해제")
    fun unlockMeeting(
        @PathVariable meetingId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<SetlistMeetingResponse> = ApiResponse.success(setlistMeetingService.unlockMeeting(meetingId, memberId))
}
