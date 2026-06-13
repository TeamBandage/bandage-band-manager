package com.bandage.bandmanager.domain.performance.dto.res

import com.bandage.bandmanager.domain.performance.model.PerformanceSetlist
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "공연 셋리스트 매핑 응답")
data class PerformanceSetlistResponse(
    @Schema(description = "PerformanceSetlist 매핑 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val performanceSetlistId: UUID,
    @Schema(description = "Setlist ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val setlistId: UUID,
) {
    companion object {
        fun of(ps: PerformanceSetlist): PerformanceSetlistResponse =
            PerformanceSetlistResponse(
                performanceSetlistId = ps.id,
                setlistId = ps.setlistId,
            )
    }
}
