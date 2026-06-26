package com.bandage.bandmanager.domain.member.dto.res

import com.bandage.bandmanager.domain.member.model.Member
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 다른 도메인 응답에 회원 정보를 임베드할 때 쓰는 경량 DTO.
 * email 등 개인정보는 제외하고 식별·표시에 필요한 최소 정보만 노출한다.
 */
@Schema(description = "회원 요약 정보 (임베드용)")
data class MemberSummary(
    @Schema(description = "회원 고유 식별자 (Long)", example = "1")
    val memberId: Long,
    @Schema(description = "회원 이름", example = "홍길동")
    val name: String,
    @Schema(description = "프로필 이미지 URL (nullable)", example = "https://cdn.example.com/profile/member/1/uuid.jpg")
    val profileImg: String? = null,
) {
    companion object {
        fun of(
            member: Member,
            profileImgUrl: String?,
        ): MemberSummary =
            MemberSummary(
                memberId = member.id,
                name = member.name,
                profileImg = profileImgUrl,
            )
    }
}
