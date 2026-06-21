package com.bandage.bandmanager.domain.performance.dto.res

import com.bandage.bandmanager.domain.performance.model.PerformancePoster
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "공연 포스터 응답")
data class PerformancePosterResponse(
    @Schema(description = "포스터 고유 식별자 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val posterId: UUID,
    @Schema(description = "공연 고유 식별자 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val performanceId: UUID,
    @Schema(description = "포스터 이미지 S3 URL", example = "https://bucket.s3.ap-northeast-2.amazonaws.com/posters/abc.png")
    val s3Url: String,
    @Schema(description = "포스터 설명", example = "TuNA 정기공연 메인 포스터")
    val description: String?,
) {
    companion object {
        fun of(poster: PerformancePoster): PerformancePosterResponse =
            PerformancePosterResponse(
                posterId = poster.id,
                performanceId = poster.performance.id,
                s3Url = poster.s3Url,
                description = poster.description,
            )
    }
}
