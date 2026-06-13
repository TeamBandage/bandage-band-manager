package com.bandage.bandmanager.domain.performance.dto.res

import com.bandage.bandmanager.domain.band.model.enums.BandRole
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "공연 참여 밴드 + 소속 멤버 요약")
data class PerformanceBandSummary(
    @Schema(description = "밴드 고유 식별자", example = "550e8400-e29b-41d4-a716-446655440000")
    val bandId: UUID,
    @Schema(description = "밴드 이름", example = "TuNA")
    val bandName: String,
    @Schema(description = "밴드 소속 멤버 목록")
    val members: List<PerformanceBandMemberSummary>,
)

@Schema(description = "공연 응답에 포함되는 밴드 멤버 요약")
data class PerformanceBandMemberSummary(
    @Schema(description = "회원 ID", example = "1")
    val userId: Long,
    @Schema(description = "이름", example = "홍길동")
    val name: String,
    @Schema(description = "프로필 이미지 URL", nullable = true)
    val profileImg: String? = null,
    @Schema(description = "밴드 내 역할", example = "LEADER")
    val role: BandRole,
)
