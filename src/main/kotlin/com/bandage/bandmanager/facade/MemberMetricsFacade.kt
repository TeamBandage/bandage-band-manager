package com.bandage.bandmanager.facade

import com.bandage.bandmanager.domain.band.repository.BandMemberRepository
import com.bandage.bandmanager.domain.jam.repository.JamParticipantRepository
import com.bandage.bandmanager.domain.member.dto.res.MemberMetricsResponse
import com.bandage.bandmanager.domain.performance.repository.PerformanceRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class MemberMetricsFacade(
    private val bandMemberRepository: BandMemberRepository,
    private val jamParticipantRepository: JamParticipantRepository,
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
            upcomingJamCount = jamParticipantRepository.countUpcomingJamsByMember(memberId, now),
            upcomingPerformanceCount = upcomingPerformanceCount,
            sessionCount = jamParticipantRepository.countSessionsByMember(memberId),
        )
    }
}
