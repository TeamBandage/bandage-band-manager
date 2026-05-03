package com.bandage.v1.domain.schedule.dto.res

import com.bandage.v1.domain.schedule.model.ScheduleBlock
import java.time.LocalDate
import java.util.UUID

data class ScheduleBlockResponse(
    val blockId: UUID,
    val songId: UUID,
    val date: LocalDate,
    val startSlot: Int,
    val durationSlots: Int,
    val pinned: Boolean,
    val paletteIndex: Int?,
    val songTitleOverride: String?,
    val note: String?,
) {
    companion object {
        fun from(block: ScheduleBlock): ScheduleBlockResponse =
            ScheduleBlockResponse(
                blockId = block.id,
                songId = block.songId,
                date = block.date,
                startSlot = block.startSlot,
                durationSlots = block.durationSlots,
                pinned = block.pinned,
                paletteIndex = block.paletteIndex,
                songTitleOverride = block.songTitleOverride,
                note = block.note,
            )
    }
}
