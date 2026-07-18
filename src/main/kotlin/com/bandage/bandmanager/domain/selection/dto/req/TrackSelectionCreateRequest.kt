package com.bandage.bandmanager.domain.selection.dto.req

import com.bandage.bandmanager.domain.selection.dto.PracticeWindowDto
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

@Schema(description = "선곡 생성 요청")
data class TrackSelectionCreateRequest(
    @field:NotBlank
    @Schema(description = "선곡 제목", example = "여름 페스티벌 셋리스트")
    val title: String,
    @Schema(description = "참여 밴드 ID 목록 (0..N, 밴드 없이 개인으로만 구성 가능)")
    val bandIds: List<UUID> = emptyList(),
    @field:NotNull
    @Schema(description = "매니저 회원 ID")
    val managerId: Long,
    @Schema(description = "참여자 회원 ID 목록 (매니저 포함)")
    val participantUserIds: List<Long> = emptyList(),
    @field:Valid
    @Schema(description = "합주 가능 기간")
    val practiceWindow: PracticeWindowDto? = null,
)
