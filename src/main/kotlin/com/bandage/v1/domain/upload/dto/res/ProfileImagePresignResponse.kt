package com.bandage.v1.domain.upload.dto.res

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "프로필 이미지 업로드용 presigned URL 발급 응답")
data class ProfileImagePresignResponse(
    @Schema(description = "PUT 업로드용 presigned URL", example = "https://bucket.s3.ap-northeast-2.amazonaws.com/...")
    val uploadUrl: String,
    @Schema(description = "업로드 후 PATCH 시 profileImg 필드에 저장할 객체 키", example = "profile/band/550e.../uuid.jpg")
    val objectKey: String,
    @Schema(description = "presigned URL 만료까지 남은 시간(초)", example = "300")
    val expiresInSeconds: Long,
)
