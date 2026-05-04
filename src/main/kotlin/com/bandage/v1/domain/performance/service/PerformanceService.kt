package com.bandage.v1.domain.performance.service

import com.bandage.v1.domain.band.repository.BandMemberRepository
import com.bandage.v1.domain.band.repository.BandRepository
import com.bandage.v1.domain.member.repository.MemberRepository
import com.bandage.v1.domain.performance.dto.req.PerformanceBandAddRequest
import com.bandage.v1.domain.performance.dto.req.PerformanceCreateRequest
import com.bandage.v1.domain.performance.dto.req.PerformancePagingQuery
import com.bandage.v1.domain.performance.dto.req.PerformancePracticeAddRequest
import com.bandage.v1.domain.performance.dto.req.PerformanceSearchQuery
import com.bandage.v1.domain.performance.dto.req.PerformanceUpdateRequest
import com.bandage.v1.domain.performance.dto.res.PerformanceBandMemberSummary
import com.bandage.v1.domain.performance.dto.res.PerformanceBandResponse
import com.bandage.v1.domain.performance.dto.res.PerformanceBandSummary
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
import com.bandage.v1.global.infra.s3.CloudFrontUrlResolver
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
    private val bandMemberRepository: BandMemberRepository,
    private val bandRepository: BandRepository,
    private val memberRepository: MemberRepository,
    private val cloudFrontUrlResolver: CloudFrontUrlResolver,
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
        (request.bandIds ?: emptyList()).forEach { bandId ->
            performanceBandRepository.save(PerformanceBand.create(performance = performance, bandId = bandId))
        }
        performanceManagerRepository.save(PerformanceManager.create(performance = performance, member = memberId))
        return PerformanceResponse.of(performance)
    }

    fun getPerformances(query: PerformancePagingQuery): CursorResponse<PerformanceListResponse, UUID> {
        val result = performanceRepository.findAllByPaging(query.lastId, query.pageSize)
        val bandSummaries = buildBandSummaries(result.content.flatMap { p -> p.bands.map { it.bandId } })
        return CursorResponse(
            content = result.content.map { PerformanceListResponse.of(it, bandSummaries) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    fun getPerformancesByBand(
        bandId: UUID,
        query: PerformancePagingQuery,
    ): CursorResponse<PerformanceListResponse, UUID> {
        val result = performanceRepository.findAllByBandIdAndPaging(bandId, query.lastId, query.pageSize)
        val bandSummaries = buildBandSummaries(result.content.flatMap { p -> p.bands.map { it.bandId } })
        return CursorResponse(
            content = result.content.map { PerformanceListResponse.of(it, bandSummaries) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    fun getMyPerformancesByCursor(
        memberId: Long,
        query: PerformancePagingQuery,
    ): CursorResponse<PerformanceListResponse, UUID> {
        val bandIds = bandMemberRepository.findAllBandIdsByMember(memberId)
        if (bandIds.isEmpty()) {
            return CursorResponse(content = emptyList(), nextCursor = null, hasNext = false)
        }
        val result = performanceRepository.findAllByBandIdsAndPaging(bandIds, query.lastId, query.pageSize)
        val bandSummaries = buildBandSummaries(result.content.flatMap { p -> p.bands.map { it.bandId } })
        return CursorResponse(
            content = result.content.map { PerformanceListResponse.of(it, bandSummaries) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    fun searchPerformancesByCursor(query: PerformanceSearchQuery): CursorResponse<PerformanceListResponse, UUID> {
        val result = performanceRepository.searchByTitleAndPaging(query.keyword, query.lastId, query.pageSize)
        val bandSummaries = buildBandSummaries(result.content.flatMap { p -> p.bands.map { it.bandId } })
        return CursorResponse(
            content = result.content.map { PerformanceListResponse.of(it, bandSummaries) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    fun getPerformanceDetail(performanceId: UUID): PerformanceDetailResponse {
        val performance = getPerformance(performanceId)
        val bandSummaries = buildBandSummaries(performance.bands.map { it.bandId })
        return PerformanceDetailResponse.of(performance, bandSummaries)
    }

    private fun buildBandSummaries(bandIds: Collection<UUID>): Map<UUID, PerformanceBandSummary> {
        if (bandIds.isEmpty()) return emptyMap()
        val distinctBandIds = bandIds.toSet()
        val bands = bandRepository.findAllById(distinctBandIds)
        val bandMembers = bandMemberRepository.findAllByBandIdIn(distinctBandIds)
        val members = memberRepository.findAllById(bandMembers.map { it.member }.toSet()).associateBy { it.id }
        val membersByBandId = bandMembers.groupBy { it.band.id }
        return bands.associate { band ->
            band.id to
                PerformanceBandSummary(
                    bandId = band.id,
                    bandName = band.name,
                    members =
                        membersByBandId[band.id].orEmpty().mapNotNull { bm ->
                            members[bm.member]?.let {
                                PerformanceBandMemberSummary(
                                    userId = it.id,
                                    name = it.name,
                                    profileImg = cloudFrontUrlResolver.resolveOrNull(it.profileImg),
                                    role = bm.role,
                                )
                            }
                        },
                )
        }
    }

    @Transactional
    fun updatePerformance(
        performanceId: UUID,
        request: PerformanceUpdateRequest,
        memberId: Long,
    ) {
        val performance = getPerformance(performanceId)
        validateIsManager(performance, memberId)
        request.title?.let { performance.updateTitle(it) }
        if (request.startAt != null || request.durationMinutes != null) {
            performance.updateTimeInfo(
                startAt = request.startAt ?: performance.timeInfo.startAt,
                durationMinutes = request.durationMinutes ?: performance.timeInfo.durationMinutes,
            )
        }
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

    @Transactional
    fun removePractice(
        performanceId: UUID,
        practiceId: UUID,
        memberId: Long,
    ) {
        val performance = getPerformance(performanceId)
        validateIsManager(performance, memberId)
        val performancePractice =
            performancePracticeRepository.findByPerformanceAndPracticeId(performance, practiceId)
                ?: throw BusinessException(ErrorCode.PERFORMANCE_PRACTICE_NOT_FOUND)
        performancePracticeRepository.delete(performancePractice)
    }

    @Transactional
    fun addBands(
        performanceId: UUID,
        request: PerformanceBandAddRequest,
        memberId: Long,
    ): List<PerformanceBandResponse> {
        val performance = getPerformance(performanceId)
        validateIsManager(performance, memberId)
        return request.bandIds.distinct().mapNotNull { bandId ->
            if (performanceBandRepository.existsByPerformanceAndBandId(performance, bandId)) return@mapNotNull null
            if (!bandRepository.existsById(bandId)) throw BusinessException(ErrorCode.BAND_NOT_FOUND)
            val pb = performanceBandRepository.save(PerformanceBand.create(performance = performance, bandId = bandId))
            PerformanceBandResponse.of(pb)
        }
    }

    @Transactional
    fun removeBand(
        performanceId: UUID,
        bandId: UUID,
        memberId: Long,
    ) {
        val performance = getPerformance(performanceId)
        validateIsManager(performance, memberId)
        val pb =
            performanceBandRepository.findByPerformanceAndBandId(performance, bandId)
                ?: throw BusinessException(ErrorCode.BAND_NOT_FOUND)
        performanceBandRepository.delete(pb)
    }

    @Transactional
    fun deletePerformance(
        performanceId: UUID,
        memberId: Long,
    ) {
        val performance = getPerformance(performanceId)
        validateIsManager(performance, memberId)
        performance.practices.forEach { it.practice.markAsDeleted(memberId) }
        performance.markAsDeleted(memberId)
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
