package com.bandage.v1.facade

import com.bandage.v1.domain.band.repository.BandMemberRepository
import com.bandage.v1.domain.member.dto.res.MemberMetricsResponse
import com.bandage.v1.domain.performance.repository.PerformanceRepository
import com.bandage.v1.domain.practice.repository.PracticeParticipantRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class MemberMetricsFacade(
    private val bandMemberRepository: BandMemberRepository,
    private val practiceParticipantRepository: PracticeParticipantRepository,
    private val performanceRepository: PerformanceRepository,
) {
    @Transactional
    fun getMemberMetrics(memberId: Long): MemberMetricsResponse {
        val now = LocalDateTime.now()
        val bandIds = bandMemberRepository.findAllBandIdsByMember(memberId)
        val upcomingPerformanceCount =
            if (bandIds.isEmpty()) 0L else performanceRepository.countUpcomingByBandIds(bandIds, now)
        return MemberMetricsResponse(
            bandCount = bandMemberRepository.countByMember(memberId),
            upcomingPracticeCount = practiceParticipantRepository.countUpcomingPracticesByMember(memberId, now),
            upcomingPerformanceCount = upcomingPerformanceCount,
            sessionCount = practiceParticipantRepository.countSessionsByMember(memberId),
        )
    }
}
