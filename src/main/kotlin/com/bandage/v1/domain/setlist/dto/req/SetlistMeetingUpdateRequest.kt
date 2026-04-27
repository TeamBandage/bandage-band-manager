package com.bandage.v1.domain.setlist.dto.req

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "선곡 회의 수정 요청")
data class SetlistMeetingUpdateRequest(
    @Schema(description = "회의 제목")
    val title: String?,
    @Schema(description = "변경할 매니저 회원 ID")
    val managerId: Long?,
)
