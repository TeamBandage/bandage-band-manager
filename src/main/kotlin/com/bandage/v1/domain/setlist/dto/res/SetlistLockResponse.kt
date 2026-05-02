package com.bandage.v1.domain.setlist.dto.res

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "선곡 회의 잠금 응답")
data class SetlistLockResponse(
    val lockedAt: LocalDateTime,
    @Schema(description = "선곡 항목 - 합주곡 매핑")
    val songs: List<SetlistLockSongMapping>,
)

data class SetlistLockSongMapping(
    val setlistItemId: UUID,
    val practiceSongId: UUID?,
)
