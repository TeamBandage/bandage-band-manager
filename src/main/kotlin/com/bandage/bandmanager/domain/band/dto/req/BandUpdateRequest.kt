package com.bandage.bandmanager.domain.band.dto.req

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "밴드 정보 수정 요청 (부분 수정)")
data class BandUpdateRequest(
    @Schema(description = "밴드 이름", example = "TuNA")
    val name: String? = null,
    @Schema(description = "밴드 상세 설명", example = "성균관대학교 락밴드입니다.")
    val description: String? = null,
    @Schema(description = "프로필 이미지 URL", example = "https://cdn/...jpg")
    val profileImg: String? = null,
)
