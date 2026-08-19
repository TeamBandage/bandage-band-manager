package com.bandage.bandmanager.domain.setlist.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.util.UUID

@Schema(description = "셋리스트 타이틀 검색 쿼리")
data class SetlistTitleSearchQuery(
    @field:NotBlank
    @Schema(description = "타이틀 검색어(대소문자 무시, 부분 일치)", example = "정기공연")
    val title: String,
    @Schema(description = "마지막으로 조회된 셋리스트 ID (커서)")
    val lastId: UUID?,
    @field:Min(1)
    @field:Max(100)
    @Schema(description = "페이지 크기", example = "20")
    val pageSize: Int = 20,
)
