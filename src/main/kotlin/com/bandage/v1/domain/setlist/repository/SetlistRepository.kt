package com.bandage.v1.domain.setlist.repository

import com.bandage.v1.domain.setlist.model.Setlist
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SetlistRepository :
    JpaRepository<Setlist, UUID>,
    SetlistRepositoryCustom {
    fun findAllByTrackSelectionId(trackSelectionId: UUID): List<Setlist>
}
