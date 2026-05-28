package com.bandage.v1.domain.setlist.repository

import com.bandage.v1.domain.setlist.model.SetlistBand
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SetlistBandRepository : JpaRepository<SetlistBand, UUID> {
    fun findAllBySetlistId(setlistId: UUID): List<SetlistBand>
}
