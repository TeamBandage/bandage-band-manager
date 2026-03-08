package com.bandage.v1.domain.band.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "밴드 생성 요청")
data class BandCreateRequest(
    @NotBlank @Schema(description = "밴드 이름", example = "TuNA")
    val name: String,
    @NotBlank @Schema(description = "밴드 상세 설명/소개", example = "성균관대학교 문과대 락밴드 TuNA 입니다.")
    val description: String,
    @Schema(description = "밴드 프로필 이미지 Url", example = "url")
    val profileImg: String,
)
