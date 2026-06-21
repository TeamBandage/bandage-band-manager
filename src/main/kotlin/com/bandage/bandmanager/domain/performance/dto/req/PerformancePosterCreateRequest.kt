package com.bandage.bandmanager.domain.performance.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

@Schema(description = "공연 포스터 등록 요청")
data class PerformancePosterCreateRequest(
    @field:NotNull
    @Schema(description = "공연 고유 식별자 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val performanceId: UUID,
    @field:NotBlank
    @Schema(description = "포스터 이미지 S3 URL", example = "https://bucket.s3.ap-northeast-2.amazonaws.com/posters/abc.png")
    val s3Url: String,
    @Schema(description = "포스터 설명", example = "TuNA 정기공연 메인 포스터")
    val description: String?,
)
