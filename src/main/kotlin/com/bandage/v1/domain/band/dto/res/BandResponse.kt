package com.bandage.v1.domain.band.dto.res

import com.bandage.v1.domain.band.model.Band
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "밴드 API 기본 응답")
data class BandResponse(
    @Schema(description = "밴드 고유 식별자 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val bandId: UUID,
    @Schema(description = "밴드 이름", example = "TuNA")
    val bandName: String,
) {
    companion object {
        fun of(band: Band): BandResponse = BandResponse(bandId = band.id, bandName = band.name)
    }
}
