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

    /** 여러 공연의 참여 셋리스트를 일괄 조회(후임 후보를 위한 밴드 탐색용). */
    fun findAllByPerformanceIn(performances: Collection<Performance>): List<PerformanceSetlist>
}
