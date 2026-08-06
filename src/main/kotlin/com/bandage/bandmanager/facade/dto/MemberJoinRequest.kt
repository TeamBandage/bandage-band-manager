package com.bandage.bandmanager.facade.dto

import com.bandage.bandmanager.domain.auth.dto.req.MemberAuthCreateRequest
import com.bandage.bandmanager.domain.member.dto.req.MemberCreateRequest
import com.bandage.bandmanager.global.common.constants.PasswordPolicy
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "회원 가입 요청")
data class MemberJoinRequest(
    @NotBlank
    @Email
    @Schema(description = "회원 이메일", example = "member@google.com")
    val email: String,
    @NotBlank
    @Size(min = PasswordPolicy.MIN_LENGTH, message = "비밀번호는 8자 이상이어야 합니다.")
    @Schema(description = "회원 비밀번호 (8자 이상)", example = "pw123456")
    val password: String,
    @NotBlank @Schema(description = "회원 이름", example = "홍길동")
    val name: String,
) {
    fun toMemberCreateRequest(): MemberCreateRequest =
        MemberCreateRequest(
            email = this.email,
            name = this.name,
        )

    fun toMemberAuthCreateRequest(memberId: Long): MemberAuthCreateRequest =
        MemberAuthCreateRequest(
            memberId = memberId,
            email = this.email,
            rawPassword = this.password,
        )
}
