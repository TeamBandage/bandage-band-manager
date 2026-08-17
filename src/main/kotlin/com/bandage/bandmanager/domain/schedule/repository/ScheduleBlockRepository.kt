package com.bandage.bandmanager.domain.schedule.repository

import com.bandage.bandmanager.domain.schedule.model.ScheduleBlock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ScheduleBlockRepository : JpaRepository<ScheduleBlock, UUID> {
    fun findAllByBoardId(boardId: UUID): List<ScheduleBlock>

    /** 자동배치가 보존해야 하는 고정 블록. pinned = false 인 블록은 재배치 대상이라 제외된다. */
    fun findAllByBoardIdAndPinned(
        boardId: UUID,
        pinned: Boolean,
    ): List<ScheduleBlock>

    fun deleteAllByBoardIdAndPinned(
        boardId: UUID,
        pinned: Boolean,
    )

    fun deleteAllByBoardId(boardId: UUID)
}
