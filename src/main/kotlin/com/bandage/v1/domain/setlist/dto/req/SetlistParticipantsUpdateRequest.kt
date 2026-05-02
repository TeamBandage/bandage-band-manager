package com.bandage.v1.domain.setlist.dto.req

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "선곡 회의 참여자 변경 요청")
data class SetlistParticipantsUpdateRequest(
    @Schema(description = "추가할 회원 ID 목록", example = "[1, 2, 3]")
    val add: List<Long> = emptyList(),
    @Schema(description = "제거할 회원 ID 목록", example = "[4, 5]")
    val remove: List<Long> = emptyList(),
)
