package com.bandage.v1.domain.performance.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import java.util.UUID

@Schema(description = "공연 합주곡 리스트 추가 요청")
data class PerformancePracticeAddRequest(
    @NotEmpty
    @Schema(description = "추가할 합주 아이디 목록", example = "[\"550e8400-e29b-41d4-a716-446655440000\"]")
    val practiceIds: List<UUID>,
)
