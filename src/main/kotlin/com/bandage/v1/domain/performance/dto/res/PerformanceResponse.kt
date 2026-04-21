package com.bandage.v1.domain.performance.dto.res

import com.bandage.v1.domain.performance.model.Performance
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "공연 기본 응답")
data class PerformanceResponse(
    @Schema(description = "공연 고유 식별자 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val performanceId: UUID,
    @Schema(description = "공연 제목", example = "TuNA 정기공연")
    val title: String,
) {
    companion object {
        fun of(performance: Performance): PerformanceResponse =
            PerformanceResponse(
                performanceId = performance.id,
                title = performance.title,
            )
    }
}
