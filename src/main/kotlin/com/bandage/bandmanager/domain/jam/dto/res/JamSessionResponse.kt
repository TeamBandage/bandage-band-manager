package com.bandage.bandmanager.domain.jam.dto.res

import com.bandage.bandmanager.domain.member.dto.res.MemberSummary
import com.bandage.bandmanager.global.common.domain.SessionDef
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "합주 세션 응답")
data class JamSessionResponse(
    @Schema(description = "세션 토큰", example = "G")
    val sessionId: String,
    @Schema(description = "세션 이름", example = "기타")
    val label: String,
    @Schema(description = "표시용 약어", example = "G")
    val short: String,
    @Schema(description = "커스텀 세션 여부", example = "false")
    val custom: Boolean,
    @Schema(description = "배정된 참여자 목록")
    val participants: List<MemberSummary>,
) {
    companion object {
        fun of(
            def: SessionDef,
            participants: List<MemberSummary>,
        ): JamSessionResponse =
            JamSessionResponse(
                sessionId = def.sessionId,
                label = def.label,
                short = def.short,
                custom = def.custom,
                participants = participants,
            )
    }
}
