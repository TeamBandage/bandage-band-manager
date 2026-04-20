package com.bandage.v1.domain.performance.repository

import com.bandage.v1.domain.performance.model.Performance
import com.bandage.v1.domain.performance.model.PerformanceManager
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PerformanceManagerRepository : JpaRepository<PerformanceManager, UUID> {
    fun existsByPerformanceAndMember(
        performance: Performance,
        member: Long,
    ): Boolean
}
