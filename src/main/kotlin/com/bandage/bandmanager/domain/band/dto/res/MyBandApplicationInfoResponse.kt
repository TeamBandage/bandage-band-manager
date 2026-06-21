package com.bandage.bandmanager.domain.band.dto.res

import com.bandage.bandmanager.domain.band.model.BandApplication
import com.bandage.bandmanager.domain.band.model.enums.ApplicationStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "내 밴드 가입 신청 항목 (신청 정보 + 대상 밴드 정보)")
data class MyBandApplicationInfoResponse(
    @Schema(description = "밴드 가입 신청 고유 식별자 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val bandApplicationId: UUID,
    @Schema(description = "신청 대상 밴드 고유 식별자 (UUID)", example = "550e8400-e29b-41d4-a716-446655440001")
    val bandId: UUID,
    @Schema(description = "신청 대상 밴드 이름", example = "TuNA")
    val bandName: String,
    @Schema(description = "신청 대상 밴드 프로필 이미지 URL (없으면 null)", example = "https://cdn/...jpg")
    val bandProfileImg: String? = null,
    @Schema(description = "가입 신청 처리 상태", example = "PENDING")
    val status: ApplicationStatus,
    @Schema(description = "신청 일시", example = "2026-04-26T12:34:56")
    val appliedAt: LocalDateTime,
) {
    companion object {
        fun of(
            application: BandApplication,
            bandProfileImgUrl: String?,
        ): MyBandApplicationInfoResponse =
            MyBandApplicationInfoResponse(
                bandApplicationId = application.id,
                bandId = application.band.id,
                bandName = application.band.name,
                bandProfileImg = bandProfileImgUrl,
                status = application.status,
                appliedAt = application.createdAt,
            )
    }
}
