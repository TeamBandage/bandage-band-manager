package com.bandage.bandmanager.domain.member.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Schema(description = "회원 프로필 이미지 업로드용 presigned URL 발급 요청")
data class MemberProfileImagePresignRequest(
    @field:NotBlank
    @Schema(description = "업로드할 파일의 Content-Type", example = "image/jpeg")
    val contentType: String,
    @field:NotBlank
    @Schema(description = "파일 확장자(점 제외, 소문자)", example = "jpg")
    val ext: String,
    @field:NotNull
    @field:Min(1)
    @Schema(description = "업로드할 파일의 정확한 바이트 크기 (최대 5MB = 5,242,880 bytes)", example = "1048576")
    val contentLength: Long,
)
