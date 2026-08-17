package com.bandage.bandmanager.domain.schedule.repository

import com.bandage.bandmanager.domain.schedule.model.ScheduleBlockTrack
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ScheduleBlockTrackRepository : JpaRepository<ScheduleBlockTrack, UUID> {
    fun findAllByBlockIdOrderByOrdinalAsc(blockId: UUID): List<ScheduleBlockTrack>

    fun findAllByBlockIdIn(blockIds: Collection<UUID>): List<ScheduleBlockTrack>

    fun deleteAllByBlockId(blockId: UUID)

    fun deleteAllByBlockIdIn(blockIds: Collection<UUID>)

    /**
     * 보드 안에서 트랙별로 배치된 블록 수를 집계한다.
     *
     * 배치 현황을 별도 테이블에 적재하지 않고 이 집계로 대신한다. 수동 배치든 자동 배치든
     * 블록이 곧 배치 결과이므로, 집계가 항상 실제 시간표와 일치한다.
     * 한 번도 배치되지 않은 트랙은 결과에 나타나지 않으므로 호출부에서 0 으로 채운다.
     */
    @Query(
        """
        SELECT bt.setlistTrackId AS trackId, COUNT(bt) AS placementCount
        FROM ScheduleBlockTrack bt
        WHERE bt.block.board.id = :boardId
        GROUP BY bt.setlistTrackId
        """,
    )
    fun countPlacementsByBoardId(
        @Param("boardId") boardId: UUID,
    ): List<TrackPlacementCountRow>
}

interface TrackPlacementCountRow {
    fun getTrackId(): UUID

    fun getPlacementCount(): Long
}
