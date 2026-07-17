package com.bandage.bandmanager.domain.jam.repository

import com.bandage.bandmanager.domain.jam.model.Jam
import com.bandage.bandmanager.domain.jam.model.JamParticipant
import com.bandage.bandmanager.domain.jam.model.JamParticipantSession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface JamParticipantSessionRepository : JpaRepository<JamParticipantSession, UUID> {
    fun findAllByJamParticipant(jamParticipant: JamParticipant): List<JamParticipantSession>

    fun findByJamParticipantAndSessionId(
        jamParticipant: JamParticipant,
        sessionId: String,
    ): JamParticipantSession?

    fun existsByJamParticipantJamAndSessionId(
        jam: Jam,
        sessionId: String,
    ): Boolean

    fun existsByJamParticipantAndSessionId(
        jamParticipant: JamParticipant,
        sessionId: String,
    ): Boolean

    fun deleteAllByJamParticipantJamAndSessionIdIn(
        jam: Jam,
        sessionIds: Collection<String>,
    )
}
