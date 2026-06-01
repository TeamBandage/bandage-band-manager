package com.bandage.v1.domain.practice.dto.req

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import java.time.LocalDateTime

@Schema(description = "셋리스트 기반 합주 생성 요청. 각 셋리스트 트랙당 합주 1건이 생성됩니다.")
data class SetlistToPracticeRequest(
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", shape = JsonFormat.Shape.STRING, timezone = "Asia/Seoul")
    @Schema(description = "생성될 합주들의 공통 시작 시간", example = "2026-03-15 18:00")
    val startAt: LocalDateTime,
    @field:Min(1)
    @Schema(description = "합주 시간 (분)", example = "60")
    val durationMinutes: Int,
    @Schema(description = "합주 장소", example = "홍대 스튜디오")
    val venue: String? = null,
)
