package com.bandage.v1.domain.practice.dto.res

import com.bandage.v1.domain.practice.model.PracticeSession
import com.bandage.v1.domain.practice.model.enums.SessionType
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "합주 세션 응답")
data class PracticeSessionResponse(
    @Schema(description = "세션 고유 식별자 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val sessionId: UUID,
    @Schema(description = "세션 레이블", example = "Guitar")
    val label: String,
    @Schema(description = "세션 타입", example = "GUITAR")
    val type: SessionType,
    @Schema(description = "배정된 참여자 정보 (미배정 시 null)")
    val participant: PracticeParticipantResponse?,
) {
    companion object {
        fun of(session: PracticeSession): PracticeSessionResponse =
            PracticeSessionResponse(
                sessionId = session.id,
                label = session.label,
                type = session.type,
                participant = session.participant?.let { PracticeParticipantResponse.of(it) },
            )
    }
}
