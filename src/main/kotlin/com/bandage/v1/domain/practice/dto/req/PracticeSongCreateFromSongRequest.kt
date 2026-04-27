package com.bandage.v1.domain.practice.dto.req

import com.bandage.v1.domain.practice.dto.Song
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.util.UUID

@Schema(description = "합주곡 생성 요청 (Song 객체 사용 — 외부 시스템에서 받아온 실제 음원 정보를 그대로 등록)")
data class PracticeSongCreateFromSongRequest(
    @Schema(
        description = "합주 ID (optional, 1:1 바인딩 대상). 미제공 시 PracticeSong 만 생성하고 Practice 바인딩은 수행하지 않음.",
        example = "550e8400-e29b-41d4-a716-446655440000",
    )
    val practiceId: UUID? = null,
    @field:Valid
    @Schema(description = "외부 시스템 곡 정보")
    val song: Song,
)
