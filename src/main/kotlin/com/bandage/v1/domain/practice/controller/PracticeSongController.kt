package com.bandage.v1.domain.practice.controller

import com.bandage.v1.domain.practice.dto.req.PracticeSongRefLinkUpsertRequest
import com.bandage.v1.domain.practice.service.PracticeService
import com.bandage.v1.global.common.constants.PathPrefix.PREFIX
import com.bandage.v1.global.common.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "practice-songs", description = "합주곡 API")
@RestController
@RequestMapping("$PREFIX/practice-songs")
class PracticeSongController(
    private val practiceService: PracticeService,
) {
    @PutMapping("/{songId}/ref-link")
    @Operation(summary = "합주곡 참조 링크 upsert API", description = "합주곡 참조 링크를 등록하거나 수정합니다.")
    fun upsertRefLink(
        @PathVariable songId: UUID,
        @Valid @RequestBody request: PracticeSongRefLinkUpsertRequest,
    ): ApiResponse<Unit> {
        practiceService.upsertPracticeSongRefLink(songId, request)
        return ApiResponse.success()
    }

    // TODO: Practice Song 참조 링크 삭제
}
