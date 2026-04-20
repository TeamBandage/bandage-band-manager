package com.bandage.v1.facade

import com.bandage.v1.domain.performance.dto.req.PerformancePracticeCreateRequest
import com.bandage.v1.domain.performance.dto.res.PerformancePracticeResponse
import com.bandage.v1.domain.performance.model.PerformancePractice
import com.bandage.v1.domain.performance.repository.PerformanceManagerRepository
import com.bandage.v1.domain.performance.repository.PerformancePracticeRepository
import com.bandage.v1.domain.performance.repository.PerformanceRepository
import com.bandage.v1.domain.practice.model.Practice
import com.bandage.v1.domain.practice.repository.PracticeRepository
import com.bandage.v1.domain.practice.repository.PracticeSongRepository
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PerformanceFacade(
    private val performanceRepository: PerformanceRepository,
    private val performanceManagerRepository: PerformanceManagerRepository,
    private val performancePracticeRepository: PerformancePracticeRepository,
    private val practiceRepository: PracticeRepository,
    private val practiceSongRepository: PracticeSongRepository,
) {
    @Transactional
    fun addNewPractice(
        performanceId: UUID,
        request: PerformancePracticeCreateRequest,
        memberId: Long,
    ): PerformancePracticeResponse {
        val performance =
            performanceRepository.findByIdOrNull(performanceId)
                ?: throw BusinessException(ErrorCode.PERFORMANCE_NOT_FOUND)
        if (!performanceManagerRepository.existsByPerformanceAndMember(performance, memberId)) {
            throw BusinessException(ErrorCode.NOT_A_PERFORMANCE_MANAGER)
        }
        val song =
            practiceSongRepository.findByIdOrNull(request.songId)
                ?: throw BusinessException(ErrorCode.PRACTICE_SONG_NOT_FOUND)
        val practice =
            practiceRepository.save(
                Practice.create(
                    title = request.title ?: song.title,
                    song = song,
                    startAt = request.startAt,
                    durationMinutes = request.durationMinutes,
                    venue = request.venue,
                ),
            )
        val performancePractice =
            performancePracticeRepository.save(
                PerformancePractice.create(performance = performance, practice = practice),
            )
        return PerformancePracticeResponse.of(performancePractice)
    }
}
