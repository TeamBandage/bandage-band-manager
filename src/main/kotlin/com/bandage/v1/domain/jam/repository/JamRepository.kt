package com.bandage.v1.domain.jam.repository

import com.bandage.v1.domain.jam.model.Jam
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface JamRepository :
    JpaRepository<Jam, UUID>,
    JamRepositoryCustom
