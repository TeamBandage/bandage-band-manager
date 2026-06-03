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
    ): ScheduleBoardResponse = TODO("Task 13(스케줄 보드 자동배치 API)에서 구현")

    @Transactional
    fun rearrangeScheduleBoard(
        meetingId: UUID,
        boardId: UUID,
        memberId: Long,
    ): ScheduleBoardResponse = TODO("Task 13(스케줄 보드 재배치 API)에서 구현")
}
