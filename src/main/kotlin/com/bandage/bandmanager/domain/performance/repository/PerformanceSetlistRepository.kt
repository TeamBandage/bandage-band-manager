package com.bandage.bandmanager.domain.performance.repository

import com.bandage.bandmanager.domain.performance.model.Performance
import com.bandage.bandmanager.domain.performance.model.PerformanceSetlist
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PerformanceSetlistRepository : JpaRepository<PerformanceSetlist, UUID> {
    fun existsByPerformanceAndSetlistId(
        performance: Performance,
        setlistId: UUID,
    ): Boolean

    fun findByPerformanceAndSetlistId(
        performance: Performance,
        setlistId: UUID,
    ): PerformanceSetlist?

    fun findAllBySetlistIdIn(setlistIds: Collection<UUID>): List<PerformanceSetlist>
}
