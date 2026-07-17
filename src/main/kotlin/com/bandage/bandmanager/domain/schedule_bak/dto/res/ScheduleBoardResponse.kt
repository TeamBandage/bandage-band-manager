package com.bandage.bandmanager.domain.schedule_bak.dto.res

import com.bandage.bandmanager.domain.schedule_bak.dto.ScheduleBoardConstraintsDto
import com.bandage.bandmanager.domain.schedule_bak.model.ScheduleBlock
import com.bandage.bandmanager.domain.schedule_bak.model.ScheduleBoard
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class ScheduleBoardResponse(
    val boardId: UUID,
    val performanceId: UUID,
    val name: String,
    val paletteSeed: Int?,
    val confirmed: Boolean,
    val constraints: ScheduleBoardConstraintsDto,
    val windowFrom: LocalDate?,
    val windowTo: LocalDate?,
    val blocks: List<ScheduleBlockResponse>,
    val version: Long,
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
                performanceId = board.performanceId,
                name = board.name,
                paletteSeed = board.paletteSeed,
                confirmed = board.confirmed,
                constraints = ScheduleBoardConstraintsDto.from(board.constraints),
                windowFrom = board.windowFrom,
                windowTo = board.windowTo,
                blocks = blocks.map { ScheduleBlockResponse.from(it, trackIdsByBlock[it.id].orEmpty()) },
                version = board.version,
                createdAt = board.createdAt,
            )
    }
}
