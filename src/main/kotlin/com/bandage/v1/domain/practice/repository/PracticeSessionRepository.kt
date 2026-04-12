package com.bandage.v1.domain.practice.repository

import com.bandage.v1.domain.practice.model.Practice
import com.bandage.v1.domain.practice.model.PracticeSession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PracticeSessionRepository : JpaRepository<PracticeSession, UUID> {
    fun findByIdAndPractice(
        id: UUID,
        practice: Practice,
    ): PracticeSession?
}
