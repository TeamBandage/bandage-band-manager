package com.bandage.bandmanager.domain.schedule_bak.model

import com.github.f4b6a3.uuid.UuidCreator
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.util.UUID

/**
 * ScheduleBlock(+반복 전개 인스턴스) → 확정 생성된 Jam 추적 매핑.
 *
 * confirmBoard 시 어떤 블록이 어떤 Jam 들을 만들었는지 기록하여, unconfirm/재확정 시 추적·정리에 사용한다.
 * occurrenceDate 는 반복 전개된 개별 발생일(단발성이면 블록 date 와 동일).
 * 파생 추적 테이블이므로 감사/소프트삭제 필드를 두지 않는다.
 */
@Entity
@Table(
    name = "p_schedule_block_jam",
    indexes = [
        Index(name = "idx_schedule_block_jam_block", columnList = "schedule_block_id"),
        Index(name = "idx_schedule_block_jam_board", columnList = "schedule_board_id"),
    ],
)
open class ScheduleBlockJam(
    scheduleBlockId: UUID,
    scheduleBoardId: UUID,
    jamId: UUID,
) {
    @Id
    @Column(name = "schedule_block_jam_id")
    val id: UUID = UuidCreator.getTimeOrderedEpoch()

    @Column(name = "schedule_block_id", nullable = false)
    val scheduleBlockId: UUID = scheduleBlockId

    @Column(name = "schedule_board_id", nullable = false)
    val scheduleBoardId: UUID = scheduleBoardId

    @Column(name = "jam_id", nullable = false)
    val jamId: UUID = jamId

    companion object {
        fun create(
            scheduleBlockId: UUID,
            scheduleBoardId: UUID,
            jamId: UUID,
        ): ScheduleBlockJam =
            ScheduleBlockJam(
                scheduleBlockId = scheduleBlockId,
                scheduleBoardId = scheduleBoardId,
                jamId = jamId,
            )
    }
}
