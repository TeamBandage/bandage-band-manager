package com.bandage.v1.domain.practice.dto.res

import com.bandage.v1.domain.practice.model.Practice
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "합주 API 기본 응답")
data class PracticeResponse(
    @Schema(description = "합주 고유 식별자 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val practiceId: UUID,
    @Schema(description = "합주 타이틀", example = "TuNA")
    val practiceTitle: String,
) {
    companion object {
        fun of(practice: Practice): PracticeResponse = PracticeResponse(practiceId = practice.id, practiceTitle = practice.title)
    }
}
