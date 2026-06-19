package com.bandage.bandmanager.domain.performance.repository

import com.bandage.bandmanager.domain.performance.model.Performance
import com.bandage.bandmanager.domain.performance.model.PerformanceManager
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PerformanceManagerRepository : JpaRepository<PerformanceManager, UUID> {
    fun existsByPerformanceAndMember(
        performance: Performance,
        member: Long,
    ): Boolean

    fun findByPerformanceAndMember(
        performance: Performance,
        member: Long,
    ): PerformanceManager?

    /** 회원이 보유한 모든 공연 권한 레코드를 공연과 함께 일괄 조회(소프트삭제되지 않은 공연만). */
    @Query("SELECT pm FROM PerformanceManager pm JOIN FETCH pm.performance WHERE pm.member = :memberId")
    fun findAllByMemberFetchPerformance(
        @Param("memberId") memberId: Long,
    ): List<PerformanceManager>

    /** 여러 공연의 모든 권한 레코드를 일괄 조회(후임 후보 산정용). */
    fun findAllByPerformanceIn(performances: Collection<Performance>): List<PerformanceManager>
}
