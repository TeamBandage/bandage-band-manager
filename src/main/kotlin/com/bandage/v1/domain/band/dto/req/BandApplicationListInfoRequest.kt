package com.bandage.v1.domain.band.dto.req

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "밴드 가입 신청 목록 조회 요청")
data class BandApplicationListInfoRequest(
    @NotBlank @Schema(description = "밴드 고유 식별자 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val bandId: String,
    @Schema(description = "application", example = "성균관대학교 문과대 락밴드 TuNA 입니다.")
    val status: String,
)
