package com.bandage.v1.domain.performance.repository

import com.bandage.v1.domain.performance.model.Performance
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PerformanceRepository : JpaRepository<Performance, UUID>
