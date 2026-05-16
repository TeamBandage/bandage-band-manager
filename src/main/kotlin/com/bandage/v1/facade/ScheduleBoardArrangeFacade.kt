package com.bandage.v1.facade

import com.bandage.v1.domain.schedule.dto.res.ScheduleBoardResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ScheduleBoardArrangeFacade {
    @Transactional
    fun setupInitialScheduleBoard(
        meetingId: UUID,
        memberId: Long,
        suggestionQty: Int,
    ): ScheduleBoardResponse = ScheduleBoardResponse.of()

    @Transactional
    fun rearrangeScheduleBoard(
        meetingId: UUID,
        boardId: UUID,
        memberId: Long,
    ): ScheduleBoardResponse = ScheduleBoardResponse.of()
}
