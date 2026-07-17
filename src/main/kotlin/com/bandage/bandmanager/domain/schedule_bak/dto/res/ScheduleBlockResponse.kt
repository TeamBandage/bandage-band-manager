package com.bandage.bandmanager.domain.schedule_bak.dto.res

import com.bandage.bandmanager.domain.schedule_bak.model.PlacementOrigin
import com.bandage.bandmanager.domain.schedule_bak.model.RecurrenceFreq
import com.bandage.bandmanager.domain.schedule_bak.model.ScheduleBlock
import java.time.LocalDate
import java.util.UUID

data class ScheduleBlockResponse(
    val blockId: UUID,
    val trackIds: List<UUID>,
    val date: LocalDate,
    val startSlot: Int,
    val durationSlots: Int,
    val pinned: Boolean,
    val paletteIndex: Int?,
    val titleOverride: String?,
    val note: String?,
    val recurrence: RecurrenceDto,
    val placementOrigin: PlacementOrigin,
) {
    data class RecurrenceDto(
        val freq: RecurrenceFreq,
        val interval: Int,
        val until: LocalDate?,
        val count: Int?,
    )

    companion object {
        fun from(
            block: ScheduleBlock,
            trackIds: List<UUID>,
        ): ScheduleBlockResponse =
            ScheduleBlockResponse(
                blockId = block.id,
                trackIds = trackIds,
                date = block.date,
                startSlot = block.startSlot,
                durationSlots = block.durationSlots,
                pinned = block.pinned,
                paletteIndex = block.paletteIndex,
                titleOverride = block.titleOverride,
                note = block.note,
                recurrence =
                    RecurrenceDto(
                        freq = block.recurrenceRule.freq,
                        interval = block.recurrenceRule.interval,
                        until = block.recurrenceRule.until,
                        count = block.recurrenceRule.count,
                    ),
                placementOrigin = block.placementOrigin,
            )
    }
}
