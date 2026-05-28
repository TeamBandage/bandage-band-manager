package com.bandage.v1.domain.selection.dto.res

import com.bandage.v1.domain.selection.dto.PracticeWindowDto
import com.bandage.v1.domain.selection.model.TrackSelection
import com.bandage.v1.domain.selection.model.TrackSelectionMember
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
    val practiceWindow: PracticeWindowDto,
    val lockedAt: LocalDateTime?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun of(
            m: TrackSelection,
            bandIds: List<UUID>,
            members: List<TrackSelectionMember>,
        ): TrackSelectionDetailResponse =
            TrackSelectionDetailResponse(
                selectionId = m.id,
                bandIds = bandIds,
                title = m.title,
                managerId = m.managerId,
                participants = members.map { ParticipantResponse.of(it) },
                practiceWindow = PracticeWindowDto.of(m.practiceWindow),
                lockedAt = m.lockedAt,
                createdAt = m.createdAt,
                updatedAt = m.lastModifiedAt,
            )
    }
}

@Schema(description = "선곡 참여자 응답")
data class ParticipantResponse(
    val memberId: Long,
    val bandIds: List<UUID>,
) {
    companion object {
        fun of(member: TrackSelectionMember): ParticipantResponse =
            ParticipantResponse(
                memberId = member.memberId,
                bandIds = member.bandIds.toList(),
            )
    }
}
