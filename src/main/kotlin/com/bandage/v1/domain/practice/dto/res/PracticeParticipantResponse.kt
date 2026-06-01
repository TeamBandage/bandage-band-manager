package com.bandage.v1.domain.practice.dto.res

import com.bandage.v1.domain.practice.model.PracticeParticipant
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "합주 참여자 응답")
data class PracticeParticipantResponse(
    @Schema(description = "참여자 고유 식별자 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val participantId: UUID,
    @Schema(description = "세션 토큰(SessionDef.sessionId)", example = "G")
    val sessionId: String,
    @Schema(description = "회원 고유 식별자 (Long)", example = "1")
    val memberId: Long,
) {
    companion object {
        fun of(participant: PracticeParticipant): PracticeParticipantResponse =
            PracticeParticipantResponse(
                participantId = participant.id,
                sessionId = participant.sessionId,
                memberId = participant.member,
            )
    }
}
