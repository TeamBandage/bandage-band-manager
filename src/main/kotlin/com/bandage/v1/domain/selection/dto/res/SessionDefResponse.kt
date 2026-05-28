package com.bandage.v1.domain.selection.dto.res

import com.bandage.v1.global.common.domain.SessionDef

data class SessionDefResponse(
    val sessionId: String,
    val label: String,
    val short: String,
    val need: Int,
    val custom: Boolean,
    val applicants: List<Long>,
    val confirmed: List<Long>,
) {
    companion object {
        fun of(
            def: SessionDef,
            applicants: List<Long>,
            confirmed: List<Long>,
        ): SessionDefResponse =
            SessionDefResponse(
                sessionId = def.sessionId,
                label = def.label,
                short = def.short,
                need = def.need,
                custom = def.custom,
                applicants = applicants,
                confirmed = confirmed,
            )
    }
}
