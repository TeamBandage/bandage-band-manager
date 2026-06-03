package com.bandage.v1.domain.jam.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.time.LocalDateTime
import java.util.UUID

/**
 * 확정된 Jam 이 점유하는 "멤버별 시간 구간"을 나타내는 추적용 테이블.
 *
 * Jam 생명주기에 맞춰 [com.bandage.v1.domain.jam.service.JamReservationSyncService] 가
 * delete-reinsert 방식으로 동기화한다. 가용성 계산(AvailabilityCalculator)에서
 * 멤버 간 합주 시간 충돌을 빠르게 조회하기 위한 비정규화 테이블이므로 감사/소프트삭제 필드를 두지 않는다.
 *
 * 시간은 시스템 전반(TimeInfoUnit/MemberSchedule)과 정합하도록 LocalDateTime 으로 표현한다.
 */
@Entity
@Table(
    name = "p_jam_reservation",
    indexes = [
        Index(name = "idx_jam_reservation_member_time", columnList = "member_id, start_at, end_at"),
    ],
)
open class JamReservation(
    memberId: Long,
    jamId: UUID,
    startAt: LocalDateTime,
    endAt: LocalDateTime,
) {
    @Id
    @Column(name = "jam_reservation_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @Column(name = "member_id", nullable = false)
    val memberId: Long = memberId

    @Column(name = "jam_id", nullable = false)
    val jamId: UUID = jamId

    @Column(name = "start_at", nullable = false)
    val startAt: LocalDateTime = startAt

    @Column(name = "end_at", nullable = false)
    val endAt: LocalDateTime = endAt

    companion object {
        fun create(
            memberId: Long,
            jamId: UUID,
            startAt: LocalDateTime,
            endAt: LocalDateTime,
        ): JamReservation =
            JamReservation(
                memberId = memberId,
                jamId = jamId,
                startAt = startAt,
                endAt = endAt,
            )
    }
}
