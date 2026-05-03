package com.bandage.v1.domain.practice.repository

import com.bandage.v1.domain.practice.model.Practice
import com.bandage.v1.domain.practice.model.PracticeParticipant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface PracticeParticipantRepository : JpaRepository<PracticeParticipant, UUID> {
    fun findByPracticeAndMember(
        practice: Practice,
        member: Long,
    ): PracticeParticipant?

    fun existsByPracticeAndMember(
        practice: Practice,
        member: Long,
    ): Boolean

    @Query(
        "SELECT COUNT(pp) FROM PracticeParticipant pp " +
            "WHERE pp.member = :memberId AND pp.practice.timeInfo.startAt > :now",
    )
    fun countUpcomingPracticesByMember(
        @Param("memberId") memberId: Long,
        @Param("now") now: LocalDateTime,
    ): Long

    @Query(
        "SELECT COUNT(ps) FROM PracticeSession ps " +
            "WHERE ps.participant IS NOT NULL AND ps.participant.member = :memberId",
    )
    fun countSessionsByMember(
        @Param("memberId") memberId: Long,
    ): Long
}
