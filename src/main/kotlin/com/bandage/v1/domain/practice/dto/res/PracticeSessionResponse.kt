package com.bandage.v1.domain.practice.dto.res

import com.bandage.v1.global.common.domain.SessionDef
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "합주 세션 응답")
data class PracticeSessionResponse(
    @Schema(description = "세션 토큰", example = "G")
    val sessionId: String,
    @Schema(description = "세션 이름", example = "기타")
    val label: String,
    @Schema(description = "표시용 약어", example = "G")
    val short: String,
    @Schema(description = "정원", example = "1")
    val need: Int,
    @Schema(description = "커스텀 세션 여부", example = "false")
    val custom: Boolean,
    @Schema(description = "배정된 참여자 회원 ID 목록")
    val participants: List<Long>,
) {
    companion object {
        fun of(
            def: SessionDef,
            participants: List<Long>,
        ): PracticeSessionResponse =
            PracticeSessionResponse(
                sessionId = def.sessionId,
                label = def.label,
                short = def.short,
                need = def.need,
                custom = def.custom,
                participants = participants,
            )
    }
}
