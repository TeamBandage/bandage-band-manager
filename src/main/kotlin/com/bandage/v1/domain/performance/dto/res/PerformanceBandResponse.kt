package com.bandage.v1.domain.performance.dto.res

import com.bandage.v1.domain.performance.model.PerformanceBand
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "공연 참여 밴드 매핑 응답 (FE-API-017)")
data class PerformanceBandResponse(
    @Schema(description = "PerformanceBand 매핑 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val performanceBandId: UUID,
    @Schema(description = "Band ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val bandId: UUID,
) {
    companion object {
        fun of(pb: PerformanceBand): PerformanceBandResponse =
            PerformanceBandResponse(
                performanceBandId = pb.id,
                bandId = pb.bandId,
            )
    }
}
