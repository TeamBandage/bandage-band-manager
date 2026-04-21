package com.bandage.v1.domain.performance.repository

import com.bandage.v1.domain.performance.model.Performance
import com.bandage.v1.domain.performance.model.PerformancePractice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PerformancePracticeRepository : JpaRepository<PerformancePractice, UUID> {
    fun findByPerformanceAndPracticeId(
        performance: Performance,
        practiceId: UUID,
    ): PerformancePractice?

    fun existsByPerformanceAndPracticeId(
        performance: Performance,
        practiceId: UUID,
    ): Boolean
}
