package com.bandage.v1.domain.setlist.controller

import com.bandage.v1.domain.setlist.dto.req.SetlistCreateRequest
import com.bandage.v1.domain.setlist.dto.res.SetlistResponse
import com.bandage.v1.facade.SetlistCreateFacade
import com.bandage.v1.global.common.constants.PathPrefix.PREFIX
import com.bandage.v1.global.common.response.ApiResponse
import com.bandage.v1.global.security.annotation.CurrentMemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "setlists", description = "셋리스트 API")
@RestController
@RequestMapping("$PREFIX/setlists")
class SetlistController(
    private val setlistCreateFacade: SetlistCreateFacade,
) {
    @PostMapping
    @Operation(summary = "셋리스트 생성", description = "잠금된 선곡(TrackSelection)에서 선택된 트랙들을 모아 셋리스트를 생성합니다.")
    fun createSetlist(
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: SetlistCreateRequest,
    ): ApiResponse<SetlistResponse> = ApiResponse.success(setlistCreateFacade.createSetlist(memberId, request))
}
