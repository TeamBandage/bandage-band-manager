package com.bandage.bandmanager.domain.selection.dto.res

import com.bandage.bandmanager.domain.member.dto.res.MemberSummary
import com.bandage.bandmanager.global.common.domain.SessionDef
import io.swagger.v3.oas.annotations.media.Schema

data class SessionDefResponse(
    val sessionId: String,
    val label: String,
    val short: String,
    val custom: Boolean,
    @Schema(description = "지원자 회원 목록")
    val applicants: List<MemberSummary>,
    @Schema(description = "확정자 회원 목록")
    val confirmed: List<MemberSummary>,
) {
    companion object {
        fun of(
            def: SessionDef,
            applicants: List<MemberSummary>,
            confirmed: List<MemberSummary>,
        ): SessionDefResponse =
            SessionDefResponse(
                sessionId = def.sessionId,
                label = def.label,
                short = def.short,
                custom = def.custom,
                applicants = applicants,
                confirmed = confirmed,
            )
    }
}
