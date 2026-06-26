package com.bandage.bandmanager.domain.jam.dto.res

import com.bandage.bandmanager.domain.jam.model.JamParticipant
import com.bandage.bandmanager.domain.member.dto.res.MemberSummary
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "합주 참여자 응답")
data class JamParticipantResponse(
    @Schema(description = "참여자 고유 식별자 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val participantId: UUID,
    @Schema(description = "세션 토큰(SessionDef.sessionId). 세션 미배정 소속 참여자는 null", example = "G")
    val sessionId: String?,
    @Schema(description = "참여자 회원 정보 (탈퇴 회원이면 null)")
    val member: MemberSummary?,
) {
    companion object {
        fun of(
            participant: JamParticipant,
            member: MemberSummary?,
        ): JamParticipantResponse =
            JamParticipantResponse(
                participantId = participant.id,
                sessionId = participant.sessionId,
                member = member,
            )
    }
}
