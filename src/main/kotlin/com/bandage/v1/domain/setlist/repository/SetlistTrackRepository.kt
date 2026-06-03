package com.bandage.v1.domain.setlist.repository

import com.bandage.v1.domain.setlist.model.Setlist
import com.bandage.v1.domain.setlist.model.SetlistTrack
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SetlistTrackRepository :
    JpaRepository<SetlistTrack, UUID>,
    SetlistTrackRepositoryCustom {
    fun findAllBySetlist(setlist: Setlist): List<SetlistTrack>

    fun findAllBySetlistIdIn(setlistIds: Collection<UUID>): List<SetlistTrack>
}
