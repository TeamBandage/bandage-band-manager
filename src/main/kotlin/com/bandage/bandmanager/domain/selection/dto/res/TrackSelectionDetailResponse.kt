package com.bandage.bandmanager.domain.selection.dto.res

import com.bandage.bandmanager.domain.member.dto.res.MemberSummary
import com.bandage.bandmanager.domain.selection.model.TrackSelection
import com.bandage.bandmanager.domain.selection.model.TrackSelectionMember
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "선곡 상세 응답")
data class TrackSelectionDetailResponse(
    val selectionId: UUID,
    val bandIds: List<UUID>,
    val title: String,
    val managerId: Long,
    @Schema(description = "참여자 목록 (멤버ID + 소속 밴드ID 목록)")
    val participants: List<ParticipantResponse>,
    val lockedAt: LocalDateTime?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun of(
            m: TrackSelection,
            bandIds: List<UUID>,
            members: List<TrackSelectionMember>,
            memberInfos: Map<Long, MemberSummary>,
        ): TrackSelectionDetailResponse =
            TrackSelectionDetailResponse(
                selectionId = m.id,
                bandIds = bandIds,
                title = m.title,
                managerId = m.managerId,
                participants = members.map { ParticipantResponse.of(it, memberInfos[it.memberId], m.managerId) },
                lockedAt = m.lockedAt,
                createdAt = m.createdAt,
                updatedAt = m.lastModifiedAt,
            )
    }
}

@Schema(description = "선곡 참여자 응답")
data class ParticipantResponse(
    @Schema(description = "참여자 회원 정보 (탈퇴 회원이면 null)")
    val member: MemberSummary?,
    val bandIds: List<UUID>,
    @Schema(description = "이 참여자가 선곡 회의의 매니저인지 여부")
    val isManager: Boolean,
) {
    companion object {
        fun of(
            member: TrackSelectionMember,
            memberInfo: MemberSummary?,
            managerId: Long,
        ): ParticipantResponse =
            ParticipantResponse(
                member = memberInfo,
                bandIds = member.bandIds.toList(),
                isManager = member.memberId == managerId,
            )
    }
}
