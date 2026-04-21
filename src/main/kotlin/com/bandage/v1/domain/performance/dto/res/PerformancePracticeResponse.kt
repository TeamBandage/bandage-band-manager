package com.bandage.v1.domain.performance.dto.res

import com.bandage.v1.domain.performance.model.PerformancePractice
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "공연 합주곡 응답")
data class PerformancePracticeResponse(
    @Schema(description = "공연 합주 연결 고유 식별자 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val performancePracticeId: UUID,
    @Schema(description = "합주 고유 식별자 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val practiceId: UUID,
) {
    companion object {
        fun of(performancePractice: PerformancePractice): PerformancePracticeResponse =
            PerformancePracticeResponse(
                performancePracticeId = performancePractice.id,
                practiceId = performancePractice.practice.id,
            )
    }
}
