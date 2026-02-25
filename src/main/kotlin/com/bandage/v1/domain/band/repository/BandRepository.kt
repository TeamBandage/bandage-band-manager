package com.bandage.v1.domain.band.repository

import com.bandage.v1.domain.band.model.Band
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BandRepository :
    JpaRepository<Band, UUID>,
    BandRepositoryCustom {
    fun existsByName(name: String): Boolean
}
