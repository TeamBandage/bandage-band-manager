package com.bandage.bandmanager.domain.performance.dto.req

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공연 포스터 설명 수정 요청")
data class PerformancePosterUpdateRequest(
    @Schema(description = "포스터 설명 (null 전달 시 설명 제거)", example = "TuNA 정기공연 메인 포스터")
    val description: String?,
)
