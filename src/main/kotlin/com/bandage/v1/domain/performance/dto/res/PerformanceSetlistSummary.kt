package com.bandage.v1.domain.performance.dto.res

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "공연 셋리스트 요약 (참여 밴드 메타 포함)")
data class PerformanceSetlistSummary(
    @Schema(description = "셋리스트 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val setlistId: UUID,
    @Schema(description = "셋리스트 제목", example = "TuNA 정기공연 셋리스트")
    val title: String,
    @Schema(description = "셋리스트에 참여하는 밴드 + 멤버 목록")
    val bands: List<PerformanceBandSummary>,
)
