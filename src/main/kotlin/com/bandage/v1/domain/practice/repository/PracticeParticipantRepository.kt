package com.bandage.v1.domain.practice.repository

import com.bandage.v1.domain.practice.model.Practice
import com.bandage.v1.domain.practice.model.PracticeParticipant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PracticeParticipantRepository : JpaRepository<PracticeParticipant, UUID> {
    fun findByPracticeAndMember(
        practice: Practice,
        member: Long,
    ): PracticeParticipant?

    fun existsByPracticeAndMember(
        practice: Practice,
        member: Long,
    ): Boolean
}
