package com.bandage.v1.domain.schedule.dto.res

import com.bandage.v1.domain.schedule.dto.ScheduleBoardConstraintsDto
import com.bandage.v1.domain.schedule.model.ScheduleBlock
import com.bandage.v1.domain.schedule.model.ScheduleBoard
import java.time.LocalDateTime
import java.util.UUID

data class ScheduleBoardResponse(
    val boardId: UUID,
    val meetingId: UUID,
    val name: String,
    val paletteSeed: Int?,
    val confirmed: Boolean,
    val constraints: ScheduleBoardConstraintsDto,
    val blocks: List<ScheduleBlockResponse>,
    val version: Long,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun of(
            board: ScheduleBoard,
            blocks: List<ScheduleBlock>,
        ): ScheduleBoardResponse =
            ScheduleBoardResponse(
                boardId = board.id,
                meetingId = board.meetingId,
                name = board.name,
                paletteSeed = board.paletteSeed,
                confirmed = board.confirmed,
                constraints = ScheduleBoardConstraintsDto.from(board.constraints),
                blocks = blocks.map { ScheduleBlockResponse.from(it) },
                version = board.version,
                createdAt = board.createdAt,
            )
    }
}
