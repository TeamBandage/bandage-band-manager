package com.bandage.bandmanager.domain.setlist.dto.res

import com.bandage.bandmanager.domain.member.dto.res.MemberSummary
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 * 셋리스트 참여자 응답(BD-226, BD-180).
 *
 * 셋리스트에는 별도 멤버 테이블이 없고 참여는 SetlistTrackParticipant(트랙×세션×멤버)로 정의된다.
 * 한 멤버가 여러 트랙·세션에 배정될 수 있으므로 세션 정보를 목록으로 제공한다.
 * 화면에서 `V.참여자1, G1.참여자2` 형태로 표시하기 위해 세션 이름과 약어를 함께 반환한다.
 */
@Schema(description = "셋리스트 참여자 응답")
data class SetlistParticipantResponse(
    @Schema(description = "참여자 회원 정보 (탈퇴 회원이면 null)")
    val member: MemberSummary?,
    @Schema(description = "이 참여자가 셋리스트의 매니저인지 여부")
    val isManager: Boolean,
    @Schema(description = "이 참여자가 배정된 세션 목록 (트랙 단위)")
    val sessions: List<SetlistParticipantSessionResponse>,
)

@Schema(description = "셋리스트 참여자의 세션 배정 정보")
data class SetlistParticipantSessionResponse(
    @Schema(description = "배정된 트랙 ID")
    val setlistTrackId: UUID,
    @Schema(description = "세션 토큰", example = "G")
    val sessionId: String,
    @Schema(description = "세션 이름", example = "GUITAR")
    val label: String,
    @Schema(description = "표시용 약어", example = "G1")
    val short: String,
)
