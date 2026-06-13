package com.bandage.bandmanager.domain.jam.repository

import com.bandage.bandmanager.domain.jam.model.Jam
import com.bandage.bandmanager.domain.jam.model.JamParticipant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface JamParticipantRepository : JpaRepository<JamParticipant, UUID> {
    fun findByIdAndJam(
        id: UUID,
        jam: Jam,
    ): JamParticipant?

    fun findAllByJam(jam: Jam): List<JamParticipant>

    fun existsByJamAndSessionIdAndMember(
        jam: Jam,
        sessionId: String,
        member: Long,
    ): Boolean

    @Query(
        "SELECT COUNT(pp) FROM JamParticipant pp " +
            "WHERE pp.member = :memberId AND pp.jam.timeInfo.startAt > :now",
    )
    fun countUpcomingJamsByMember(
        @Param("memberId") memberId: Long,
        @Param("now") now: LocalDateTime,
    ): Long

    @Query("SELECT COUNT(pp) FROM JamParticipant pp WHERE pp.member = :memberId")
    fun countSessionsByMember(
        @Param("memberId") memberId: Long,
    ): Long
}
