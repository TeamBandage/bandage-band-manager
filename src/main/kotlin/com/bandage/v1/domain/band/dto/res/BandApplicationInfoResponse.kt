package com.bandage.v1.domain.band.dto.res

import com.bandage.v1.domain.band.model.BandApplication
import com.bandage.v1.domain.band.model.enums.ApplicationStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "밴드 가입 신청 단건 조회 응답")
data class BandApplicationInfoResponse(
    @Schema(description = "밴드 가입 신청 고유 식별자 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val bandApplicationId: UUID,
    @Schema(description = "가입 신청 회원 고유 식별자 (Long)", example = "1")
    val memberId: Long,
    @Schema(description = "가입 신청 처리 상태", example = "PENDING")
    val status: ApplicationStatus,
) {
    companion object {
        fun of(application: BandApplication): BandApplicationInfoResponse =
            BandApplicationInfoResponse(
                bandApplicationId = application.id,
                memberId = application.member,
                status = application.status,
            )
    }
}
