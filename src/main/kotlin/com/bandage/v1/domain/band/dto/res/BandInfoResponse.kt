package com.bandage.v1.domain.band.dto.res

import com.bandage.v1.domain.band.model.Band
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "밴드 API 상세 조회 응답")
data class BandInfoResponse(
    @Schema(description = "밴드 고유 식별자 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val bandId: UUID,
    @Schema(description = "밴드 이름", example = "TuNA")
    val bandName: String,
    @Schema(description = "밴드 상세 설명", example = "성균관대학교 문과대 락밴드 TuNA 입니다.")
    val description: String?,
    @Schema(description = "밴드 프로필 이미지 url", example = "image_url")
    val profileImg: String?,
) {
    companion object {
        fun of(band: Band): BandInfoResponse =
            BandInfoResponse(
                bandId = band.id,
                bandName = band.name,
                description = band.description,
                profileImg = band.profileImg,
            )
    }
}
