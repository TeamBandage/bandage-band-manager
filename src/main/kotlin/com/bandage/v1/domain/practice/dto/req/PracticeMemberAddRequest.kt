package com.bandage.v1.domain.practice.dto.req

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "합주 멤버 추가 요청")
data class PracticeMemberAddRequest(
    @Schema(description = "추가할 회원 아이디", example = "1")
    val memberId: Long,
)
