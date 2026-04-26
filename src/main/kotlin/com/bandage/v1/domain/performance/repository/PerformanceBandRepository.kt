package com.bandage.v1.domain.performance.repository

import com.bandage.v1.domain.performance.model.Performance
import com.bandage.v1.domain.performance.model.PerformanceBand
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PerformanceBandRepository : JpaRepository<PerformanceBand, UUID> {
    fun existsByPerformanceAndBandId(
        performance: Performance,
        bandId: UUID,
    ): Boolean

    fun findByPerformanceAndBandId(
        performance: Performance,
        bandId: UUID,
    ): PerformanceBand?
}
