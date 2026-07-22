package com.bandage.bandmanager.domain.schedule.dto.res

import com.bandage.bandmanager.domain.schedule.model.ScheduleBlock
import com.bandage.bandmanager.domain.schedule.model.enums.PlacementOrigin
import com.bandage.bandmanager.domain.schedule.model.enums.RecurrenceFreq
import java.time.LocalDate
import java.util.UUID

data class ScheduleBlockResponse(
    val blockId: UUID,
    val trackIds: List<UUID>,
    val startDate: LocalDate,
    val startSlot: Int,
    val endDate: LocalDate,
    val endSlot: Int,
    val pinned: Boolean,
    val title: String?,
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
                startDate = block.startDate,
                startSlot = block.startSlot,
                endDate = block.endDate,
                endSlot = block.endSlot,
                pinned = block.pinned,
                title = block.title,
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
