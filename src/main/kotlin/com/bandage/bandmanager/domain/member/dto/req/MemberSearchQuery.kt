package com.bandage.bandmanager.domain.member.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

@Schema(description = "회원 검색 쿼리")
data class MemberSearchQuery(
    @Schema(description = "이름/이메일 부분 일치 검색어", example = "홍길동")
    val q: String = "",
    @Schema(description = "마지막으로 조회된 회원 ID (커서)", example = "42")
    val lastId: Long?,
    @field:Min(1)
    @field:Max(100)
    @Schema(description = "페이지 크기", example = "20")
    val pageSize: Int = 20,
)
