package com.bandage.v1.domain.schedule.model

import com.bandage.v1.global.common.domain.BaseEntity
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
 * 기존 단일 songId 를 대체한다. 한 블록(시간 구간)에 여러 트랙을 묶어 함께 연습할 수 있다.
 * ordinal 로 블록 내 트랙 순서를 유지한다.
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
