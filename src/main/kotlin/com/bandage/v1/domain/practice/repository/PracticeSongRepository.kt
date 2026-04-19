package com.bandage.v1.domain.practice.repository

import com.bandage.v1.domain.practice.model.PracticeSong
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PracticeSongRepository : JpaRepository<PracticeSong, UUID> {
    fun getPracticeSongById(songId: UUID): PracticeSong?
}
