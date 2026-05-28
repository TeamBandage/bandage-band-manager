package com.bandage.v1.domain.selection.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "선곡 참여자 변경 요청")
data class SetlistParticipantsUpdateRequest(
    @Schema(description = "추가할 회원 목록 (멤버ID + 밴드ID 목록)")
    val add: List<ParticipantAddDto> = emptyList(),
    @Schema(description = "제거할 회원 ID 목록", example = "[4, 5]")
    val remove: List<Long> = emptyList(),
)

@Schema(description = "선곡 참여자 추가 항목")
data class ParticipantAddDto(
    @Schema(description = "회원 ID", example = "1")
    val memberId: Long,
    @Schema(description = "소속 밴드 ID 목록 (0..N)")
    val bandIds: List<UUID> = emptyList(),
)
