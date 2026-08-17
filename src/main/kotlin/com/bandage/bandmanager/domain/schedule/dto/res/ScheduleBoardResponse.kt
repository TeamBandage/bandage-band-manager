package com.bandage.bandmanager.domain.schedule.dto.res

import com.bandage.bandmanager.domain.schedule.model.ScheduleBlock
import com.bandage.bandmanager.domain.schedule.model.ScheduleBoard
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class ScheduleBoardResponse(
    val boardId: UUID,
    val setlistId: UUID,
    val name: String,
    val confirmed: Boolean,
    val boardTimeRangeFrom: Int,
    val boardTimeRangeTo: Int,
    val windowFrom: LocalDate?,
    val windowTo: LocalDate?,
    val blocks: List<ScheduleBlockResponse>,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun of(
            board: ScheduleBoard,
            blocks: List<ScheduleBlock>,
            trackIdsByBlock: Map<UUID, List<UUID>> = emptyMap(),
        ): ScheduleBoardResponse =
            ScheduleBoardResponse(
                boardId = board.id,
                setlistId = board.setlistId,
                name = board.name,
                confirmed = board.confirmed,
                boardTimeRangeFrom = board.boardTimeRangeFrom,
                boardTimeRangeTo = board.boardTimeRangeTo,
                windowFrom = board.windowFrom,
                windowTo = board.windowTo,
                blocks = blocks.map { ScheduleBlockResponse.from(it, trackIdsByBlock[it.id].orEmpty()) },
                createdAt = board.createdAt,
            )
    }
}
