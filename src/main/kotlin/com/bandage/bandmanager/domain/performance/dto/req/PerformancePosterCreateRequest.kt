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
    @Schema(
        description = "presigned URL 발급 응답의 objectKey (S3 객체 키)",
        example = "poster/performance/550e8400-e29b-41d4-a716-446655440000/abc.png",
    )
    val imageKey: String,
    @Schema(description = "포스터 설명", example = "TuNA 정기공연 메인 포스터")
    val description: String?,
)
