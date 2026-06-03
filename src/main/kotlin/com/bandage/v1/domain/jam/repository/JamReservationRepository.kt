package com.bandage.v1.domain.jam.repository

import com.bandage.v1.domain.jam.model.JamReservation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.UUID

interface JamReservationRepository : JpaRepository<JamReservation, UUID> {
    fun findAllByJamId(jamId: UUID): List<JamReservation>

    @Modifying
    fun deleteAllByJamId(jamId: UUID)

    fun findAllByMemberIdIn(memberIds: Collection<Long>): List<JamReservation>

    /**
     * 특정 멤버의 예약 중 [startAt, endAt) 구간과 겹치는 예약을 조회한다.
     * 겹침 조건: (예약.start < 구간.end) AND (예약.end > 구간.start)
     */
    @Query(
        """
        SELECT jr FROM JamReservation jr
        WHERE jr.memberId = :memberId
          AND jr.startAt < :endAt
          AND jr.endAt > :startAt
        """,
    )
    fun findConflictingReservations(
        @Param("memberId") memberId: Long,
        @Param("startAt") startAt: LocalDateTime,
        @Param("endAt") endAt: LocalDateTime,
    ): List<JamReservation>
}
