package com.bandage.bandmanager.domain.setlist.repository

import com.bandage.bandmanager.domain.setlist.model.SetlistTrack
import com.bandage.bandmanager.domain.setlist.model.SetlistTrackParticipant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SetlistTrackParticipantRepository : JpaRepository<SetlistTrackParticipant, UUID> {
    fun findAllByTrack(track: SetlistTrack): List<SetlistTrackParticipant>

    fun findAllByTrackIn(tracks: List<SetlistTrack>): List<SetlistTrackParticipant>

    fun deleteAllByTrack(track: SetlistTrack)
}
