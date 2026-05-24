package com.bandage.v1.domain.setlist.dto.res

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "선곡 회의 잠금 응답")
data class SetlistLockResponse(
    val lockedAt: LocalDateTime,
    @Schema(description = "선곡 회의 항목 - 합주곡 매핑 (정상 잠금된 전체 곡)")
    val songs: List<SetlistLockSongMapping>,
    @Schema(description = "곡별 itemId → practiceSongId 매핑 (편의 필드)")
    val practiceSongMap: Map<UUID, UUID>,
    @Schema(description = "재잠금 시 적용된 변경 내역")
    val diff: SetlistLockDiff,
)

data class SetlistLockSongMapping(
    val setlistMeetingItemId: UUID,
    val practiceSongId: UUID?,
)

@Schema(description = "잠금 적용 변경 내역")
data class SetlistLockDiff(
    @Schema(description = "신규 생성된 합주곡 매핑")
    val added: List<SetlistLockSongMapping>,
    @Schema(description = "변경된 합주곡 매핑 (제목/아티스트/앨범/duration)")
    val updated: List<SetlistLockSongMapping>,
    @Schema(description = "이전 잠금에 있었으나 현재 항목에서 제거된 합주곡 매핑 (현 단계에서는 빈 배열 — 정책 미확정)")
    val removed: List<SetlistLockSongMapping> = emptyList(),
)
