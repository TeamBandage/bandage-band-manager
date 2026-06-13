package com.bandage.bandmanager.facade.dto

import com.bandage.bandmanager.domain.member.model.Member
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "회원 API 기본 응답")
data class MemberResponse(
    @Schema(description = "회원 아이디", example = "1")
    val id: Long,
    @Schema(description = "회원 이메일", example = "member@gmail.com")
    val email: String,
) {
    companion object {
        fun of(member: Member): MemberResponse =
            MemberResponse(
                id = member.id!!,
                email = member.email,
            )
    }
}
