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
    fun findByIdAndPractice(
        id: UUID,
        practice: Practice,
    ): PracticeParticipant?

    fun findAllByPractice(practice: Practice): List<PracticeParticipant>

    fun existsByPracticeAndSessionIdAndMember(
        practice: Practice,
        sessionId: String,
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

    @Query("SELECT COUNT(pp) FROM PracticeParticipant pp WHERE pp.member = :memberId")
    fun countSessionsByMember(
        @Param("memberId") memberId: Long,
    ): Long
}
