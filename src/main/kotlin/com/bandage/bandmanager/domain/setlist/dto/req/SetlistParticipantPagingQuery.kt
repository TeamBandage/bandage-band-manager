package com.bandage.bandmanager.domain.setlist.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

@Schema(description = "셋리스트 참여 멤버 목록 조회 쿼리")
data class SetlistParticipantPagingQuery(
    @Schema(description = "마지막으로 조회된 참여 회원 ID (커서)", example = "42")
    val lastId: Long?,
    @field:Min(1)
    @field:Max(100)
    @Schema(description = "페이지 크기", example = "20")
    val pageSize: Int = 20,
)
