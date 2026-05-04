package com.bandage.v1.domain.upload.controller

import com.bandage.v1.domain.upload.dto.req.ProfileImagePresignRequest
import com.bandage.v1.domain.upload.dto.res.ProfileImagePresignResponse
import com.bandage.v1.domain.upload.service.ProfileImageUploadService
import com.bandage.v1.global.common.constants.PathPrefix.PREFIX
import com.bandage.v1.global.common.response.ApiResponse
import com.bandage.v1.global.security.annotation.CurrentMemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "uploads", description = "파일 업로드 API")
@RestController
@RequestMapping("$PREFIX/uploads")
class ProfileImageUploadController(
    private val profileImageUploadService: ProfileImageUploadService,
) {
    @PostMapping("/profile-image/presigned-url")
    @Operation(
        summary = "프로필 이미지 업로드용 presigned URL 발급",
        description =
            "Band/Member 프로필 이미지를 S3에 PUT 업로드하기 위한 presigned URL을 발급합니다. " +
                "응답의 objectKey 를 PATCH 시 profileImg 필드에 그대로 전달합니다.",
    )
    fun issueProfileImagePresignedUrl(
        @Valid @RequestBody request: ProfileImagePresignRequest,
        @CurrentMemberId memberId: Long,
    ): ApiResponse<ProfileImagePresignResponse> =
        ApiResponse.success(
            profileImageUploadService.issuePresignedUrl(request, memberId),
        )
}
