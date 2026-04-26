package com.bandage.v1.domain.performance.repository

import com.bandage.v1.domain.performance.model.Performance
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface PerformanceRepository :
    JpaRepository<Performance, UUID>,
    PerformanceRepositoryCustom {
    @Query(
        "SELECT COUNT(DISTINCT pb.performance.id) FROM PerformanceBand pb " +
            "WHERE pb.bandId IN :bandIds AND pb.performance.schedule.startAt > :now " +
            "AND pb.performance.deletedAt IS NULL",
    )
    fun countUpcomingByBandIds(
        @Param("bandIds") bandIds: Collection<UUID>,
        @Param("now") now: LocalDateTime,
    ): Long
}
