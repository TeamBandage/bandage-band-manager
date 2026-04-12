package com.bandage.v1.domain.practice.controller

import com.bandage.v1.domain.practice.dto.req.PracticeCreateRequest
import com.bandage.v1.domain.practice.dto.req.PracticeSessionCreateRequest
import com.bandage.v1.domain.practice.dto.res.PracticeDetailResponse
import com.bandage.v1.domain.practice.dto.res.PracticeResponse
import com.bandage.v1.domain.practice.dto.res.PracticeSessionResponse
import com.bandage.v1.domain.practice.service.PracticeService
import com.bandage.v1.global.common.constants.PathPrefix.PREFIX
import com.bandage.v1.global.common.response.ApiResponse
import com.bandage.v1.global.security.annotation.CurrentMemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
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

    @GetMapping("/{practiceId}")
    @Operation(summary = "합주 조회 API", description = "합주 상세 정보를 조회합니다.")
    fun getPractice(
        @PathVariable practiceId: UUID,
    ): ApiResponse<PracticeDetailResponse> =
        ApiResponse.success(
            practiceService.getPracticeDetail(practiceId),
        )

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
}
