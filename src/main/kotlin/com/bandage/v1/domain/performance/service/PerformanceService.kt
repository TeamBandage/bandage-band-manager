package com.bandage.v1.domain.performance.service

import com.bandage.v1.domain.performance.dto.req.PerformanceCreateRequest
import com.bandage.v1.domain.performance.dto.req.PerformancePagingQuery
import com.bandage.v1.domain.performance.dto.req.PerformancePracticeAddRequest
import com.bandage.v1.domain.performance.dto.req.PerformanceUpdateRequest
import com.bandage.v1.domain.performance.dto.res.PerformanceDetailResponse
import com.bandage.v1.domain.performance.dto.res.PerformanceListResponse
import com.bandage.v1.domain.performance.dto.res.PerformancePracticeResponse
import com.bandage.v1.domain.performance.dto.res.PerformanceResponse
import com.bandage.v1.domain.performance.model.Performance
import com.bandage.v1.domain.performance.model.PerformanceBand
import com.bandage.v1.domain.performance.model.PerformanceManager
import com.bandage.v1.domain.performance.model.PerformancePractice
import com.bandage.v1.domain.performance.repository.PerformanceBandRepository
import com.bandage.v1.domain.performance.repository.PerformanceManagerRepository
import com.bandage.v1.domain.performance.repository.PerformancePracticeRepository
import com.bandage.v1.domain.performance.repository.PerformanceRepository
import com.bandage.v1.domain.practice.model.Practice
import com.bandage.v1.domain.practice.repository.PracticeRepository
import com.bandage.v1.global.common.response.CursorResponse
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class PerformanceService(
    private val performanceRepository: PerformanceRepository,
    private val performanceBandRepository: PerformanceBandRepository,
    private val performanceManagerRepository: PerformanceManagerRepository,
    private val performancePracticeRepository: PerformancePracticeRepository,
    private val practiceRepository: PracticeRepository,
) {
    @Transactional
    fun createPerformance(
        request: PerformanceCreateRequest,
        memberId: Long,
    ): PerformanceResponse {
        val performance =
            performanceRepository.save(
                Performance.create(
                    title = request.title,
                    startAt = request.startAt,
                    durationMinutes = request.durationMinutes,
                    venue = request.venue,
                ),
            )
        request.bandIds.forEach { bandId ->
            performanceBandRepository.save(PerformanceBand.create(performance = performance, bandId = bandId))
        }
        performanceManagerRepository.save(PerformanceManager.create(performance = performance, member = memberId))
        return PerformanceResponse.of(performance)
    }

    fun getPerformances(query: PerformancePagingQuery): CursorResponse<PerformanceListResponse, UUID> {
        val result = performanceRepository.findAllByPaging(query.lastId, query.pageSize)
        return CursorResponse(
            content = result.content.map { PerformanceListResponse.of(it) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    fun getPerformancesByBand(
        bandId: UUID,
        query: PerformancePagingQuery,
    ): CursorResponse<PerformanceListResponse, UUID> {
        val result = performanceRepository.findAllByBandIdAndPaging(bandId, query.lastId, query.pageSize)
        return CursorResponse(
            content = result.content.map { PerformanceListResponse.of(it) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    fun getPerformanceDetail(performanceId: UUID): PerformanceDetailResponse = PerformanceDetailResponse.of(getPerformance(performanceId))

    @Transactional
    fun updatePerformance(
        performanceId: UUID,
        request: PerformanceUpdateRequest,
        memberId: Long,
    ) {
        val performance = getPerformance(performanceId)
        validateIsManager(performance, memberId)
        performance.updateTitle(request.title)
        performance.updateSchedule(request.startAt, request.durationMinutes)
        request.venue?.let { performance.updateVenue(it) }
    }

    @Transactional
    fun addPractices(
        performanceId: UUID,
        request: PerformancePracticeAddRequest,
        memberId: Long,
    ): List<PerformancePracticeResponse> {
        val performance = getPerformance(performanceId)
        validateIsManager(performance, memberId)
        return request.practiceIds.mapNotNull { practiceId ->
            if (performancePracticeRepository.existsByPerformanceAndPracticeId(performance, practiceId)) return@mapNotNull null
            val practice = getPractice(practiceId)
            val performancePractice =
                performancePracticeRepository.save(
                    PerformancePractice.create(performance = performance, practice = practice),
                )
            PerformancePracticeResponse.of(performancePractice)
        }
    }

    fun getPerformance(performanceId: UUID): Performance =
        performanceRepository.findByIdOrNull(performanceId)
            ?: throw BusinessException(ErrorCode.PERFORMANCE_NOT_FOUND)

    private fun getPractice(practiceId: UUID): Practice =
        practiceRepository.findByIdOrNull(practiceId)
            ?: throw BusinessException(ErrorCode.PRACTICE_NOT_FOUND)

    fun validateIsManager(
        performance: Performance,
        memberId: Long,
    ) {
        if (!performanceManagerRepository.existsByPerformanceAndMember(performance, memberId)) {
            throw BusinessException(ErrorCode.NOT_A_PERFORMANCE_MANAGER)
        }
    }
}
