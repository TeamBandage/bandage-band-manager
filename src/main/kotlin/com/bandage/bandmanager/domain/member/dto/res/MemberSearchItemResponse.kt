package com.bandage.bandmanager.domain.member.dto.res

import com.bandage.bandmanager.domain.member.model.Member
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "회원 검색 결과 아이템")
data class MemberSearchItemResponse(
    @Schema(description = "회원 고유 식별자 (Long)", example = "1")
    val memberId: Long,
    @Schema(description = "회원 이름", example = "홍길동")
    val name: String,
    @Schema(description = "회원 이메일", example = "user@bandage.test")
    val email: String,
    @Schema(description = "프로필 이미지 URL (CloudFront)", example = "https://cdn.example.com/profile/member/1/uuid.jpg")
    val profileImg: String? = null,
) {
    companion object {
        fun of(
            member: Member,
            profileImgUrl: String?,
        ): MemberSearchItemResponse =
            MemberSearchItemResponse(
                memberId = member.id,
                name = member.name,
                email = member.email,
                profileImg = profileImgUrl,
            )
    }
}
