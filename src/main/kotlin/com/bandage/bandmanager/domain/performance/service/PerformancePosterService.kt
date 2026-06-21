package com.bandage.bandmanager.domain.performance.service

import com.bandage.bandmanager.domain.performance.dto.req.PerformancePosterCreateRequest
import com.bandage.bandmanager.domain.performance.dto.req.PerformancePosterUpdateRequest
import com.bandage.bandmanager.domain.performance.dto.res.PerformancePosterResponse
import com.bandage.bandmanager.domain.performance.model.Performance
import com.bandage.bandmanager.domain.performance.model.PerformancePoster
import com.bandage.bandmanager.domain.performance.repository.PerformanceManagerRepository
import com.bandage.bandmanager.domain.performance.repository.PerformancePosterRepository
import com.bandage.bandmanager.domain.performance.repository.PerformanceRepository
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import com.bandage.bandmanager.global.infra.s3.CloudFrontUrlResolver
import com.bandage.bandmanager.global.infra.s3.ImagePresignRequest
import com.bandage.bandmanager.global.infra.s3.ImagePresignResponse
import com.bandage.bandmanager.global.infra.s3.ImagePresignSupport
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class PerformancePosterService(
    private val performancePosterRepository: PerformancePosterRepository,
    private val performanceRepository: PerformanceRepository,
    private val performanceManagerRepository: PerformanceManagerRepository,
    private val cloudFrontUrlResolver: CloudFrontUrlResolver,
    private val imagePresignSupport: ImagePresignSupport,
) {
    /** 공연 포스터 업로드용 presigned URL 발급. OWNER/MANAGER만 가능. 응답 objectKey 를 포스터 등록 시 imageKey 로 전달. */
    fun issuePresignedUrl(
        performanceId: UUID,
        request: ImagePresignRequest,
        memberId: Long,
    ): ImagePresignResponse {
        val performance = requirePerformance(performanceId)
        validateParticipant(performance, memberId)
        return imagePresignSupport.issue(request, "poster/performance/$performanceId")
    }

    @Transactional
    fun createPoster(
        request: PerformancePosterCreateRequest,
        memberId: Long,
    ): PerformancePosterResponse {
        val performance = requirePerformance(request.performanceId)
        validateParticipant(performance, memberId)
        val poster =
            performancePosterRepository.save(
                PerformancePoster.create(performance = performance, imageKey = request.imageKey, description = request.description),
            )
        return toResponse(poster)
    }

    fun getPoster(posterId: UUID): PerformancePosterResponse = toResponse(requirePoster(posterId))

    fun getPostersByPerformance(performanceId: UUID): List<PerformancePosterResponse> =
        performancePosterRepository.findAllByPerformanceIdOrderByCreatedAtDesc(performanceId).map { toResponse(it) }

    fun getAllPosters(): List<PerformancePosterResponse> =
        performancePosterRepository.findAllByOrderByCreatedAtDesc().map { toResponse(it) }

    @Transactional
    fun updateDescription(
        posterId: UUID,
        request: PerformancePosterUpdateRequest,
        memberId: Long,
    ): PerformancePosterResponse {
        val poster = requirePoster(posterId)
        validateParticipant(poster.performance, memberId)
        poster.updateDescription(request.description)
        return toResponse(poster)
    }

    @Transactional
    fun deletePoster(
        posterId: UUID,
        memberId: Long,
    ) {
        val poster = requirePoster(posterId)
        validateParticipant(poster.performance, memberId)
        performancePosterRepository.delete(poster)
    }

    private fun toResponse(poster: PerformancePoster): PerformancePosterResponse =
        PerformancePosterResponse.of(poster, cloudFrontUrlResolver.resolve(poster.imageKey))

    private fun requirePerformance(performanceId: UUID): Performance =
        performanceRepository.findByIdOrNull(performanceId)
            ?: throw BusinessException(ErrorCode.PERFORMANCE_NOT_FOUND)

    private fun requirePoster(posterId: UUID): PerformancePoster =
        performancePosterRepository.findByIdOrNull(posterId)
            ?: throw BusinessException(ErrorCode.PERFORMANCE_POSTER_NOT_FOUND)

    /** OWNER/MANAGER(공연 참여자)만 포스터를 등록/수정/삭제할 수 있다. */
    private fun validateParticipant(
        performance: Performance,
        memberId: Long,
    ) {
        if (!performanceManagerRepository.existsByPerformanceAndMember(performance, memberId)) {
            throw BusinessException(ErrorCode.NOT_A_PERFORMANCE_MANAGER)
        }
    }
}
