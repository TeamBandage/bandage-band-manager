package com.bandage.v1.domain.member.dto.res

import com.bandage.v1.domain.member.model.Member
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "회원 정보 조회 응답")
data class MemberInfoResponse(
    @Schema(description = "회원 고유 식별자 (Long)", example = "1")
    val memberId: Long,
    @Schema(description = "회원 이메일", example = "member@google.com")
    val email: String,
    @Schema(description = "회원 이름", example = "홍길동")
    val name: String,
    @Schema(description = "회원 연락처", example = "010-7707-5859")
    val contact: String,
) {
    companion object {
        fun of(member: Member): MemberInfoResponse =
            MemberInfoResponse(
                memberId = member.id,
                email = member.email,
                name = member.name,
                contact = member.contact,
            )
    }
}
