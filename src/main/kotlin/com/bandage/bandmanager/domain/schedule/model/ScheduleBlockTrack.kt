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
 * ScheduleBlock 과 SetlistTrack 의 N:M 매핑.
 *
 * 자동배치는 블록 1개에 트랙 1건만 연결한다(ordinal = 0). 같은 멤버가 한 구간에서 두 곡을
 * 동시에 연습할 수는 없으므로, 곡별로 블록을 나눠 두어야 곡별 시각이 남고 곡 단위 이동·삭제가
 * 블록 조작 하나로 끝난다.
 *
 * 여러 트랙이 한 블록에 붙는 것은 사용자가 수동으로 묶었을 때뿐이다. 이때도 실제 진행은 순차이며,
 * ordinal 은 블록 안에서의 연습 순서를 뜻한다(시각이 아니다).
 */
@Entity
@Table(
    name = "p_schedule_block_track",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_schedule_block_track",
            columnNames = ["schedule_block_id", "setlist_track_id"],
        ),
    ],
)
@SQLRestriction("deleted_at IS NULL")
open class ScheduleBlockTrack(
    id: UUID,
    block: ScheduleBlock,
    setlistTrackId: UUID,
    ordinal: Int,
) : BaseEntity() {
    @Id
    @Column(name = "schedule_block_track_id")
    val id: UUID = id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_block_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val block: ScheduleBlock = block

    @Column(name = "setlist_track_id", nullable = false)
    val setlistTrackId: UUID = setlistTrackId

    @Column(name = "ordinal", nullable = false)
    var ordinal: Int = ordinal
        protected set

    fun updateOrdinal(ordinal: Int) {
        this.ordinal = ordinal
    }

    companion object {
        fun create(
            block: ScheduleBlock,
            setlistTrackId: UUID,
            ordinal: Int,
            id: UUID = UuidCreator.getTimeOrderedEpoch(),
        ): ScheduleBlockTrack =
            ScheduleBlockTrack(
                id = id,
                block = block,
                setlistTrackId = setlistTrackId,
                ordinal = ordinal,
            )
    }
}
