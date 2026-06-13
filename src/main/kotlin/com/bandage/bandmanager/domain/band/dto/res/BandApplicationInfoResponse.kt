package com.bandage.bandmanager.domain.band.dto.res

import com.bandage.bandmanager.domain.band.model.BandApplication
import com.bandage.bandmanager.domain.band.model.enums.ApplicationStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "밴드 가입 신청 단건 조회 응답")
data class BandApplicationInfoResponse(
    @Schema(description = "밴드 가입 신청 고유 식별자 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val bandApplicationId: UUID,
    @Schema(description = "가입 신청 회원 고유 식별자 (Long)", example = "1")
    val memberId: Long,
    @Schema(description = "가입 신청 처리 상태", example = "PENDING")
    val status: ApplicationStatus,
    @Schema(description = "신청자 이름", example = "홍길동")
    val applicantName: String? = null,
    @Schema(description = "신청자 프로필 이미지 URL", example = "https://cdn/...jpg")
    val applicantProfileImg: String? = null,
    @Schema(description = "신청 일시", example = "2026-04-26T12:34:56")
    val appliedAt: LocalDateTime? = null,
) {
    companion object {
        fun of(application: BandApplication): BandApplicationInfoResponse =
            BandApplicationInfoResponse(
                bandApplicationId = application.id,
                memberId = application.member,
                status = application.status,
                appliedAt = application.createdAt,
            )

        fun of(
            application: BandApplication,
            applicantName: String?,
            applicantProfileImg: String?,
        ): BandApplicationInfoResponse =
            BandApplicationInfoResponse(
                bandApplicationId = application.id,
                memberId = application.member,
                status = application.status,
                applicantName = applicantName,
                applicantProfileImg = applicantProfileImg,
                appliedAt = application.createdAt,
            )
    }
}
