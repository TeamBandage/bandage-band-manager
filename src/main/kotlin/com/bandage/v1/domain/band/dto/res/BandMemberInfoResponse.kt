package com.bandage.v1.domain.band.dto.res

import com.bandage.v1.domain.band.model.BandMember
import com.bandage.v1.domain.band.model.enums.BandRole
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "밴드 멤버 상세 조회 응답")
data class BandMemberInfoResponse(
    @Schema(description = "밴드 멤버 고유 식별자 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val bandMemberId: UUID,
    @Schema(description = "회원 고유 식별자 (Long)", example = "1")
    val memberId: Long,
    @Schema(description = "밴드 멤버 역할", example = "MEMBER")
    val role: BandRole,
) {
    companion object {
        fun of(bandMember: BandMember): BandMemberInfoResponse =
            BandMemberInfoResponse(
                bandMemberId = bandMember.id,
                memberId = bandMember.member,
                role = bandMember.role,
            )
    }
}
