package com.bandage.bandmanager.domain.performance.repository

import com.bandage.bandmanager.domain.performance.model.Performance
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
        "SELECT COUNT(DISTINCT ps.performance.id) FROM PerformanceSetlist ps " +
            "JOIN SetlistBand sb ON sb.setlistId = ps.setlistId " +
            "WHERE sb.bandId IN :bandIds AND ps.performance.timeInfo.startAt > :now " +
            "AND ps.performance.deletedAt IS NULL",
    )
    fun countUpcomingByBandIds(
        @Param("bandIds") bandIds: Collection<UUID>,
        @Param("now") now: LocalDateTime,
    ): Long

    // 임박 공연 알림 스케줄러용: [from, to) 구간에 시작하는 공연 조회
    @Query("SELECT p FROM Performance p WHERE p.timeInfo.startAt >= :from AND p.timeInfo.startAt < :to")
    fun findUpcomingBetween(
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
    ): List<Performance>
}
