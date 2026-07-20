package com.bandage.bandmanager.domain.jam.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.LocalDate
import java.util.UUID

@Schema(description = "합주 목록 조회 쿼리")
data class JamPagingQuery(
    @Schema(description = "마지막으로 조회된 합주 ID (커서)", example = "550e8400-e29b-41d4-a716-446655440000")
    val lastId: UUID?,
    @field:Min(1)
    @field:Max(100)
    @Schema(description = "페이지 크기", example = "10")
    val pageSize: Int = 10,
    @Schema(description = "조회 시작일 (미지정 시 제한 없음)", example = "2026-07-01")
    val from: LocalDate? = null,
    @Schema(description = "조회 종료일 (미지정 시 제한 없음)", example = "2026-07-31")
    val to: LocalDate? = null,
)
