package com.bandage.bandmanager.domain.auth.dto.req

import com.bandage.bandmanager.global.common.constants.PasswordPolicy
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "회원 비밀번호 변경 요청")
data class MemberPasswordChangeRequest(
    @NotBlank @Schema(description = "기존 비밀번호", example = "originalPW123!")
    val originalPassword: String,
    @NotBlank
    @Size(min = PasswordPolicy.MIN_LENGTH, message = "비밀번호는 8자 이상이어야 합니다.")
    @Schema(description = "새 비밀번호 (8자 이상)", example = "newPW123!")
    val newPassword: String,
)
