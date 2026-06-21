package com.bandage.bandmanager.domain.performance.service

import com.bandage.bandmanager.domain.performance.dto.req.PerformancePosterCreateRequest
import com.bandage.bandmanager.domain.performance.dto.req.PerformancePosterUpdateRequest
import com.bandage.bandmanager.domain.performance.dto.res.PerformancePosterResponse
import com.bandage.bandmanager.domain.performance.model.PerformancePoster
import com.bandage.bandmanager.domain.performance.repository.PerformancePosterRepository
import com.bandage.bandmanager.domain.performance.repository.PerformanceRepository
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class PerformancePosterService(
    private val performancePosterRepository: PerformancePosterRepository,
    private val performanceRepository: PerformanceRepository,
) {
    @Transactional
    fun createPoster(request: PerformancePosterCreateRequest): PerformancePosterResponse {
        val performance =
            performanceRepository.findByIdOrNull(request.performanceId)
                ?: throw BusinessException(ErrorCode.PERFORMANCE_NOT_FOUND)
        val poster =
            performancePosterRepository.save(
                PerformancePoster.create(performance = performance, s3Url = request.s3Url, description = request.description),
            )
        return PerformancePosterResponse.of(poster)
    }

    fun getPoster(posterId: UUID): PerformancePosterResponse = PerformancePosterResponse.of(requirePoster(posterId))

    fun getPostersByPerformance(performanceId: UUID): List<PerformancePosterResponse> =
        performancePosterRepository.findAllByPerformanceIdOrderByCreatedAtDesc(performanceId).map { PerformancePosterResponse.of(it) }

    fun getAllPosters(): List<PerformancePosterResponse> =
        performancePosterRepository.findAllByOrderByCreatedAtDesc().map { PerformancePosterResponse.of(it) }

    @Transactional
    fun updateDescription(
        posterId: UUID,
        request: PerformancePosterUpdateRequest,
    ): PerformancePosterResponse {
        val poster = requirePoster(posterId)
        poster.updateDescription(request.description)
        return PerformancePosterResponse.of(poster)
    }

    @Transactional
    fun deletePoster(posterId: UUID) {
        performancePosterRepository.delete(requirePoster(posterId))
    }

    private fun requirePoster(posterId: UUID): PerformancePoster =
        performancePosterRepository.findByIdOrNull(posterId)
            ?: throw BusinessException(ErrorCode.PERFORMANCE_POSTER_NOT_FOUND)
}
