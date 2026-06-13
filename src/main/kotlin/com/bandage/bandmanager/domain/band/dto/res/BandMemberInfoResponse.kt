package com.bandage.bandmanager.domain.band.dto.res

import com.bandage.bandmanager.domain.band.model.BandMember
import com.bandage.bandmanager.domain.band.model.enums.BandRole
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
    @Schema(description = "회원 이름", example = "홍길동")
    val name: String? = null,
    @Schema(description = "회원 프로필 이미지 URL", example = "https://cdn/...jpg")
    val profileImg: String? = null,
) {
    companion object {
        fun of(bandMember: BandMember): BandMemberInfoResponse =
            BandMemberInfoResponse(
                bandMemberId = bandMember.id,
                memberId = bandMember.member,
                role = bandMember.role,
            )

        fun of(
            bandMember: BandMember,
            memberName: String?,
            profileImg: String?,
        ): BandMemberInfoResponse =
            BandMemberInfoResponse(
                bandMemberId = bandMember.id,
                memberId = bandMember.member,
                role = bandMember.role,
                name = memberName,
                profileImg = profileImg,
            )
    }
}
