package com.bandage.bandmanager.domain.schedule.model

import com.bandage.bandmanager.global.common.domain.BaseEntity
import com.github.f4b6a3.uuid.UuidCreator
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.hibernate.annotations.SQLRestriction
import java.util.UUID

/**
 * 스케줄보드 ↔ 셋리스트 트랙 단위의 자동배치 결과.
 *
 * 보드 안에서 각 트랙이 몇 회 배치됐는지를 기록한다. placementCount == 0 이면 미배치다.
 * 미배치 트랙은 블록이 존재하지 않아 ScheduleBlockTrack 으로는 표현할 수 없으므로 이 테이블이 필요하다.
 *
 * 블록의 실제 시간 정보는 ScheduleBlockTrack 이 보유하며, 여기서는 중복 저장하지 않는다.
 * 자동배치를 다시 실행하면 보드 단위로 전체 교체된다.
 */
@Entity
@Table(
    name = "p_schedule_board_setlist_track_placement",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_schedule_board_setlist_track_placement",
            columnNames = ["schedule_board_id", "setlist_track_id"],
        ),
    ],
)
@SQLRestriction("deleted_at IS NULL")
open class ScheduleBoardSetlistTrackPlacement(
    id: UUID,
    board: ScheduleBoard,
    setlistTrackId: UUID,
    placementCount: Int,
) : BaseEntity() {
    @Id
    @Column(name = "schedule_board_setlist_track_placement_id")
    val id: UUID = id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_board_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val board: ScheduleBoard = board

    @Column(name = "setlist_track_id", nullable = false)
    val setlistTrackId: UUID = setlistTrackId

    // 이 보드에서 해당 트랙이 배치된 회차 수. 0 이면 한 번도 배치되지 못했다.
    @Column(name = "placement_count", nullable = false)
    var placementCount: Int = placementCount
        protected set

    val isPlaced: Boolean get() = placementCount > 0

    companion object {
        fun create(
            board: ScheduleBoard,
            setlistTrackId: UUID,
            placementCount: Int = 0,
            id: UUID = UuidCreator.getTimeOrderedEpoch(),
        ): ScheduleBoardSetlistTrackPlacement {
            require(placementCount >= 0) { "placementCount must be >= 0, was $placementCount" }
            return ScheduleBoardSetlistTrackPlacement(
                id = id,
                board = board,
                setlistTrackId = setlistTrackId,
                placementCount = placementCount,
            )
        }
    }

    fun updatePlacementCount(count: Int) {
        require(count >= 0) { "placementCount must be >= 0, was $count" }
        this.placementCount = count
    }

    fun increasePlacementCount() {
        this.placementCount++
    }
}
