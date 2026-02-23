package com.bandage.v1.domain.band.repository

import com.bandage.v1.domain.band.model.Band
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BandRepository : JpaRepository<Band, UUID>
