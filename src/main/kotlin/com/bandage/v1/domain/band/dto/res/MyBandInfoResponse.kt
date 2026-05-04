package com.bandage.v1.domain.band.dto.res

import com.bandage.v1.domain.band.model.Band
import com.bandage.v1.domain.band.model.enums.BandRole
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "내 밴드 목록 항목 (밴드 정보 + 본인 역할)")
data class MyBandInfoResponse(
    @Schema(description = "밴드 고유 식별자 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val bandId: UUID,
    @Schema(description = "밴드 이름", example = "TuNA")
    val bandName: String,
    @Schema(description = "밴드 상세 설명")
    val description: String?,
    @Schema(description = "밴드 프로필 이미지 url")
    val profileImg: String?,
    @Schema(description = "본인의 밴드 내 역할", example = "LEADER")
    val myRole: BandRole,
) {
    companion object {
        fun of(
            band: Band,
            myRole: BandRole,
            profileImgUrl: String?,
        ): MyBandInfoResponse =
            MyBandInfoResponse(
                bandId = band.id,
                bandName = band.name,
                description = band.description,
                profileImg = profileImgUrl,
                myRole = myRole,
            )
    }
}
