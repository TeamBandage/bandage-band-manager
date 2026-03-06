package com.bandage.v1.facade.dto

import com.bandage.v1.domain.member.dto.req.MemberCreateRequest
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

@Schema(description = "회원 가입 요청")
data class MemberJoinRequest(
    @NotBlank
    @Email
    @Schema(description = "회원 이메일", example = "member@google.com")
    val email: String,
    @NotBlank @Schema(description = "회원 비밀번호", example = "pw1234")
    val password: String,
    @NotBlank @Schema(description = "회원 이름", example = "홍길동")
    val name: String,
    @NotBlank @Schema(description = "회원 연락처", example = "010-1234-5678")
    val contact: String,
) {
    fun toMemberCreateRequest(): MemberCreateRequest =
        MemberCreateRequest(
            email = this.email,
            password = this.password,
            name = this.name,
            contact = this.contact,
        )
}
