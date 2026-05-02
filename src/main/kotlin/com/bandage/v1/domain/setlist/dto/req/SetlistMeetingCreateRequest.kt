package com.bandage.v1.domain.setlist.dto.req

import com.bandage.v1.domain.setlist.model.enums.MeetingPurpose
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

@Schema(description = "선곡 회의 생성 요청")
data class SetlistMeetingCreateRequest(
    @field:NotBlank
    @Schema(description = "회의 제목", example = "여름 페스티벌 셋리스트 회의")
    val title: String,
    @field:NotNull
    @Schema(description = "회의 목적", example = "PERFORMANCE")
    val purpose: MeetingPurpose,
    @Schema(description = "공연 ID (purpose=PERFORMANCE 일 때 필수)")
    val performanceId: UUID?,
    @field:NotNull
    @Schema(description = "기준 밴드 ID")
    val bandId: UUID,
    @field:NotNull
    @Schema(description = "매니저 회원 ID")
    val managerId: Long,
    @Schema(description = "참여자 회원 ID 목록 (매니저 포함)")
    val participantUserIds: List<Long> = emptyList(),
)
