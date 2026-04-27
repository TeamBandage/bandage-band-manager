package com.bandage.v1.domain.member.dto.res

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "회원 메트릭 응답")
data class MemberMetricsResponse(
    @Schema(description = "본인이 소속된 밴드 수", example = "3")
    val bandCount: Long,
    @Schema(description = "본인이 참여한 다가오는 합주 수 (startAt > now)", example = "2")
    val upcomingPracticeCount: Long,
    @Schema(description = "본인 밴드의 다가오는 공연 수 (startAt > now)", example = "1")
    val upcomingPerformanceCount: Long,
    @Schema(description = "본인이 참여 중인 합주 세션 수", example = "5")
    val sessionCount: Long,
)
