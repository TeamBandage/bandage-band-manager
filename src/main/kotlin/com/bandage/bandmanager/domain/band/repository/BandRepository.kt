package com.bandage.bandmanager.domain.band.repository

import com.bandage.bandmanager.domain.band.model.Band
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BandRepository :
    JpaRepository<Band, UUID>,
    BandRepositoryCustom {
    fun existsByName(name: String): Boolean
}
