package com.bandage.bandmanager.domain.member.dto.res

import com.bandage.bandmanager.domain.member.model.Member
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "회원 정보 조회 응답")
data class MemberInfoResponse(
    @Schema(description = "회원 고유 식별자 (Long) — 프론트 호환용 alias", example = "1")
    val id: Long,
    @Schema(description = "회원 고유 식별자 (Long)", example = "1")
    val memberId: Long,
    @Schema(description = "회원 이메일", example = "member@google.com")
    val email: String,
    @Schema(description = "회원 이름", example = "홍길동")
    val name: String,
    @Schema(description = "회원 연락처 (소셜 로그인 가입 시 null)", example = "010-7707-5859")
    val contact: String?,
    @Schema(description = "프로필 이미지 URL (CloudFront, 없으면 null)", example = "https://cdn.example.com/profile/member/1/uuid.jpg")
    val profileImg: String? = null,
) {
    companion object {
        fun of(
            member: Member,
            profileImgUrl: String?,
        ): MemberInfoResponse =
            MemberInfoResponse(
                id = member.id,
                memberId = member.id,
                email = member.email,
                name = member.name,
                contact = member.contact,
                profileImg = profileImgUrl,
            )
    }
}
