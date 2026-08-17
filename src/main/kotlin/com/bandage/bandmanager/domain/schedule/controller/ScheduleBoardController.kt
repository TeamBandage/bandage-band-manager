package com.bandage.bandmanager.domain.schedule.controller

import com.bandage.bandmanager.domain.schedule.dto.req.ScheduleBoardCreateRequest
import com.bandage.bandmanager.domain.schedule.dto.req.ScheduleBoardUpdateRequest
import com.bandage.bandmanager.domain.schedule.dto.res.ScheduleBoardPlacementResponse
import com.bandage.bandmanager.domain.schedule.dto.res.ScheduleBoardResponse
import com.bandage.bandmanager.domain.schedule.service.ScheduleBoardService
import com.bandage.bandmanager.global.common.constants.PathPrefix
import com.bandage.bandmanager.global.common.response.ApiResponse
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

@Tag(name = "schedule-boards", description = "시간표 시안 API (셋리스트 단위)")
@RestController
@RequestMapping("${PathPrefix.PREFIX}/setlists/{setlistId}/schedule-boards")
class ScheduleBoardController(
    private val scheduleBoardService: ScheduleBoardService,
) {
    @GetMapping
    @Operation(operationId = "getBoards", summary = "시간표 시안 목록", description = "셋리스트 참여자만 호출 가능.")
    fun getBoards(
        @PathVariable setlistId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<List<ScheduleBoardResponse>> = ApiResponse.success(scheduleBoardService.getBoards(setlistId, memberId))

    @GetMapping("/{boardId}/placements")
    @Operation(
        operationId = "getBoardPlacements",
        summary = "시간표 시안의 트랙별 배치 현황",
        description = "셋리스트 참여자만 호출 가능. 셋리스트의 모든 트랙을 반환하며 미배치 트랙은 placementCount=0.",
    )
    fun getPlacements(
        @PathVariable setlistId: UUID,
        @PathVariable boardId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<List<ScheduleBoardPlacementResponse>> =
        ApiResponse.success(scheduleBoardService.getPlacements(setlistId, boardId, memberId))

    @PostMapping
    @Operation(operationId = "createBoard", summary = "시간표 시안 생성", description = "셋리스트 매니저 권한, 셋리스트당 최대 5개.")
    fun createBoard(
        @PathVariable setlistId: UUID,
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: ScheduleBoardCreateRequest,
    ): ApiResponse<ScheduleBoardResponse> = ApiResponse.success(scheduleBoardService.createBoard(setlistId, memberId, request))

    @PatchMapping("/{boardId}")
    @Operation(operationId = "updateBoard", summary = "시간표 시안 수정", description = "셋리스트 매니저 권한, confirmed=true 인 시안은 수정 불가(409).")
    fun updateBoard(
        @PathVariable setlistId: UUID,
        @PathVariable boardId: UUID,
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: ScheduleBoardUpdateRequest,
    ): ApiResponse<ScheduleBoardResponse> = ApiResponse.success(scheduleBoardService.updateBoard(setlistId, boardId, memberId, request))

    @DeleteMapping("/{boardId}")
    @Operation(operationId = "deleteBoard", summary = "시간표 시안 삭제", description = "셋리스트 매니저 권한, confirmed=true 인 시안은 삭제 불가(409).")
    fun deleteBoard(
        @PathVariable setlistId: UUID,
        @PathVariable boardId: UUID,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<Unit> {
        scheduleBoardService.deleteBoard(setlistId, boardId, memberId)
        return ApiResponse.Companion.success()
    }

    // ponytail: confirm/unconfirm 은 ScheduleConfirmFacade 를 setlist 스코프로 전환한 뒤 복원 예정(BD-172 다음 단계)
}
