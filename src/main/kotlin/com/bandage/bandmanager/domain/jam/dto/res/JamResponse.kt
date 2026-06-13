package com.bandage.bandmanager.domain.jam.dto.res

import com.bandage.bandmanager.domain.jam.model.Jam
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "합주 API 기본 응답")
data class JamResponse(
    @Schema(description = "합주 고유 식별자 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val jamId: UUID,
    @Schema(description = "합주 타이틀", example = "TuNA")
    val jamTitle: String,
) {
    companion object {
        fun of(jam: Jam): JamResponse = JamResponse(jamId = jam.id, jamTitle = jam.title)
    }
}
