package com.bandage.bandmanager.domain.upload.dto.req

import com.bandage.bandmanager.domain.upload.model.enums.UploadDomain
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

@Schema(description = "프로필 이미지 업로드용 presigned URL 발급 요청")
data class ProfileImagePresignRequest(
    @field:NotNull
    @Schema(description = "업로드 대상 도메인", example = "BAND")
    val domain: UploadDomain,
    @Schema(description = "domain=BAND 인 경우 필수, 권한 검증 대상 밴드 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val bandId: UUID? = null,
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
) {
    @AssertTrue(message = "domain=BAND 인 경우 bandId 는 필수입니다.")
    @Schema(hidden = true)
    fun isBandIdConsistent(): Boolean = domain != UploadDomain.BAND || bandId != null
}
