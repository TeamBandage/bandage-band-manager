package com.bandage.v1.domain.performance.service

import com.bandage.v1.domain.band.repository.BandMemberRepository
import com.bandage.v1.domain.band.repository.BandRepository
import com.bandage.v1.domain.member.repository.MemberRepository
import com.bandage.v1.domain.performance.dto.req.PerformanceCreateRequest
import com.bandage.v1.domain.performance.dto.req.PerformancePagingQuery
import com.bandage.v1.domain.performance.dto.req.PerformanceSearchQuery
import com.bandage.v1.domain.performance.dto.req.PerformanceSetlistAddRequest
import com.bandage.v1.domain.performance.dto.req.PerformanceUpdateRequest
import com.bandage.v1.domain.performance.dto.res.PerformanceBandMemberSummary
import com.bandage.v1.domain.performance.dto.res.PerformanceBandSummary
import com.bandage.v1.domain.performance.dto.res.PerformanceDetailResponse
import com.bandage.v1.domain.performance.dto.res.PerformanceListResponse
import com.bandage.v1.domain.performance.dto.res.PerformanceResponse
import com.bandage.v1.domain.performance.dto.res.PerformanceSetlistResponse
import com.bandage.v1.domain.performance.dto.res.PerformanceSetlistSummary
import com.bandage.v1.domain.performance.model.Performance
import com.bandage.v1.domain.performance.model.PerformanceManager
import com.bandage.v1.domain.performance.model.PerformanceSetlist
import com.bandage.v1.domain.performance.repository.PerformanceManagerRepository
import com.bandage.v1.domain.performance.repository.PerformanceRepository
import com.bandage.v1.domain.performance.repository.PerformanceSetlistRepository
import com.bandage.v1.domain.setlist.model.Setlist
import com.bandage.v1.domain.setlist.repository.SetlistBandRepository
import com.bandage.v1.domain.setlist.repository.SetlistRepository
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
    private val performanceSetlistRepository: PerformanceSetlistRepository,
    private val performanceManagerRepository: PerformanceManagerRepository,
    private val setlistRepository: SetlistRepository,
    private val setlistBandRepository: SetlistBandRepository,
    private val bandRepository: BandRepository,
    private val bandMemberRepository: BandMemberRepository,
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
        (request.setlistIds ?: emptyList()).distinct().forEach { setlistId ->
            requireSetlist(setlistId)
            performanceSetlistRepository.save(PerformanceSetlist.create(performance = performance, setlistId = setlistId))
        }
        performanceManagerRepository.save(PerformanceManager.create(performance = performance, member = memberId))
        return PerformanceResponse.of(performance)
    }

    fun getPerformances(query: PerformancePagingQuery): CursorResponse<PerformanceListResponse, UUID> {
        val result = performanceRepository.findAllByPaging(query.lastId, query.pageSize)
        val summaries = buildSetlistSummaries(result.content.flatMap { p -> p.setlists.map { it.setlistId } })
        return CursorResponse(
            content = result.content.map { PerformanceListResponse.of(it, summaries) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    fun getPerformancesByBand(
        bandId: UUID,
        query: PerformancePagingQuery,
    ): CursorResponse<PerformanceListResponse, UUID> {
        val result = performanceRepository.findAllByBandIdAndPaging(bandId, query.lastId, query.pageSize)
        val summaries = buildSetlistSummaries(result.content.flatMap { p -> p.setlists.map { it.setlistId } })
        return CursorResponse(
            content = result.content.map { PerformanceListResponse.of(it, summaries) },
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
        val summaries = buildSetlistSummaries(result.content.flatMap { p -> p.setlists.map { it.setlistId } })
        return CursorResponse(
            content = result.content.map { PerformanceListResponse.of(it, summaries) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    fun searchPerformancesByCursor(query: PerformanceSearchQuery): CursorResponse<PerformanceListResponse, UUID> {
        val result = performanceRepository.searchByTitleAndPaging(query.keyword, query.lastId, query.pageSize)
        val summaries = buildSetlistSummaries(result.content.flatMap { p -> p.setlists.map { it.setlistId } })
        return CursorResponse(
            content = result.content.map { PerformanceListResponse.of(it, summaries) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }

    fun getPerformanceDetail(performanceId: UUID): PerformanceDetailResponse {
        val performance = getPerformance(performanceId)
        val summaries = buildSetlistSummaries(performance.setlists.map { it.setlistId })
        return PerformanceDetailResponse.of(performance, summaries)
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
    fun addSetlists(
        performanceId: UUID,
        request: PerformanceSetlistAddRequest,
        memberId: Long,
    ): List<PerformanceSetlistResponse> {
        val performance = getPerformance(performanceId)
        validateIsManager(performance, memberId)
        return request.setlistIds.distinct().mapNotNull { setlistId ->
            if (performanceSetlistRepository.existsByPerformanceAndSetlistId(performance, setlistId)) return@mapNotNull null
            requireSetlist(setlistId)
            val saved = performanceSetlistRepository.save(PerformanceSetlist.create(performance = performance, setlistId = setlistId))
            PerformanceSetlistResponse.of(saved)
        }
    }

    @Transactional
    fun removeSetlist(
        performanceId: UUID,
        setlistId: UUID,
        memberId: Long,
    ) {
        val performance = getPerformance(performanceId)
        validateIsManager(performance, memberId)
        val ps =
            performanceSetlistRepository.findByPerformanceAndSetlistId(performance, setlistId)
                ?: throw BusinessException(ErrorCode.PERFORMANCE_SETLIST_NOT_FOUND)
        performanceSetlistRepository.delete(ps)
    }

    @Transactional
    fun deletePerformance(
        performanceId: UUID,
        memberId: Long,
    ) {
        val performance = getPerformance(performanceId)
        validateIsManager(performance, memberId)
        performance.markAsDeleted(memberId)
    }

    private fun buildSetlistSummaries(setlistIds: Collection<UUID>): Map<UUID, PerformanceSetlistSummary> {
        if (setlistIds.isEmpty()) return emptyMap()
        val distinctSetlistIds = setlistIds.toSet()
        val setlists = setlistRepository.findAllById(distinctSetlistIds).associateBy { it.id }
        val setlistBands = setlistBandRepository.findAllBySetlistIdIn(distinctSetlistIds)
        val bandIdsBySetlistId: Map<UUID, List<UUID>> =
            setlistBands.groupBy({ it.setlistId }, { it.bandId })
        val bandSummariesByBandId = buildBandSummaries(setlistBands.map { it.bandId })
        return distinctSetlistIds
            .mapNotNull { setlistId ->
                val setlist = setlists[setlistId] ?: return@mapNotNull null
                setlistId to
                    PerformanceSetlistSummary(
                        setlistId = setlist.id,
                        title = setlist.title,
                        bands = (bandIdsBySetlistId[setlistId] ?: emptyList()).mapNotNull { bandSummariesByBandId[it] },
                    )
            }.toMap()
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

    fun getPerformance(performanceId: UUID): Performance =
        performanceRepository.findByIdOrNull(performanceId)
            ?: throw BusinessException(ErrorCode.PERFORMANCE_NOT_FOUND)

    private fun requireSetlist(setlistId: UUID): Setlist =
        setlistRepository.findByIdOrNull(setlistId)
            ?: throw BusinessException(ErrorCode.SETLIST_NOT_FOUND)

    fun validateIsManager(
        performance: Performance,
        memberId: Long,
    ) {
        if (!performanceManagerRepository.existsByPerformanceAndMember(performance, memberId)) {
            throw BusinessException(ErrorCode.NOT_A_PERFORMANCE_MANAGER)
        }
    }
}
