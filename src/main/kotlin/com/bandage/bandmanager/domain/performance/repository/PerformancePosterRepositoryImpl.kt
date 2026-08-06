package com.bandage.bandmanager.domain.performance.repository

import com.bandage.bandmanager.domain.performance.model.PerformancePoster
import com.bandage.bandmanager.domain.performance.model.QPerformanceManager
import com.bandage.bandmanager.domain.performance.model.QPerformancePoster
import com.bandage.bandmanager.domain.performance.model.QPerformanceSetlist
import com.bandage.bandmanager.domain.setlist.model.QSetlistBand
import com.bandage.bandmanager.global.common.response.CursorResponse
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import java.util.UUID

class PerformancePosterRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : PerformancePosterRepositoryCustom {
    override fun findAllByPaging(
        performanceId: UUID?,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<PerformancePoster, UUID> {
        val qPoster = QPerformancePoster.performancePoster

        val contents =
            queryFactory
                .selectFrom(qPoster)
                .join(qPoster.performance)
                .fetchJoin()
                .where(eqPerformanceId(performanceId))
                .where(ltPosterId(lastId))
                .orderBy(qPoster.id.desc())
                .limit(pageSize.toLong() + 1)
                .fetch()

        return buildCursorResponse(contents, pageSize)
    }

    override fun findMyPostersByCursor(
        memberId: Long,
        bandIds: List<UUID>,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<PerformancePoster, UUID> {
        val qPoster = QPerformancePoster.performancePoster
        val qPerformanceSetlist = QPerformanceSetlist.performanceSetlist
        val qSetlistBand = QSetlistBand.setlistBand
        val qPerformanceManager = QPerformanceManager.performanceManager

        // 직접 참여 경로: 공연 멤버(생성자 OWNER · 초대 수락 MANAGER)는 밴드 매핑과 무관하게 항상 포함
        val viaManager =
            JPAExpressions
                .select(qPerformanceManager.performance.id)
                .from(qPerformanceManager)
                .where(qPerformanceManager.member.eq(memberId))
        var condition: BooleanExpression = qPoster.performance.id.`in`(viaManager)

        // 밴드 참여 경로: 내가 속한 밴드가 셋리스트로 참여하는 공연 — 소속 밴드가 있을 때만 합집합
        if (bandIds.isNotEmpty()) {
            val viaBand =
                JPAExpressions
                    .select(qPerformanceSetlist.performance.id)
                    .from(qPerformanceSetlist)
                    .join(qSetlistBand)
                    .on(qSetlistBand.setlistId.eq(qPerformanceSetlist.setlistId))
                    .where(qSetlistBand.bandId.`in`(bandIds))
            condition = condition.or(qPoster.performance.id.`in`(viaBand))
        }

        val contents =
            queryFactory
                .selectFrom(qPoster)
                .join(qPoster.performance)
                .fetchJoin()
                .where(condition)
                .where(ltPosterId(lastId))
                .orderBy(qPoster.id.desc())
                .limit(pageSize.toLong() + 1)
                .fetch()

        return buildCursorResponse(contents, pageSize)
    }

    private fun buildCursorResponse(
        contents: List<PerformancePoster>,
        pageSize: Int,
    ): CursorResponse<PerformancePoster, UUID> {
        val hasNext = contents.size > pageSize
        val resultContents = if (hasNext) contents.dropLast(1) else contents
        val nextCursor = if (hasNext) resultContents.lastOrNull()?.id else null
        return CursorResponse(content = resultContents, nextCursor = nextCursor, hasNext = hasNext)
    }

    private fun eqPerformanceId(performanceId: UUID?): BooleanExpression? =
        performanceId?.let {
            QPerformancePoster.performancePoster.performance.id
                .eq(it)
        }

    private fun ltPosterId(lastId: UUID?): BooleanExpression? = lastId?.let { QPerformancePoster.performancePoster.id.lt(it) }
}
