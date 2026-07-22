package com.bandage.bandmanager.domain.performance.repository

import com.bandage.bandmanager.domain.performance.model.Performance
import com.bandage.bandmanager.domain.performance.model.QPerformance
import com.bandage.bandmanager.domain.performance.model.QPerformanceManager
import com.bandage.bandmanager.domain.performance.model.QPerformanceSetlist
import com.bandage.bandmanager.domain.setlist.model.QSetlistBand
import com.bandage.bandmanager.global.common.response.CursorResponse
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDate
import java.util.UUID

class PerformanceRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : PerformanceRepositoryCustom {
    override fun findAllByPaging(
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Performance, UUID> {
        val qPerformance = QPerformance.performance

        val contents =
            queryFactory
                .selectFrom(qPerformance)
                .where(ltPerformanceId(lastId))
                .orderBy(qPerformance.id.desc())
                .limit(pageSize.toLong() + 1)
                .fetch()

        return buildCursorResponse(contents, pageSize)
    }

    override fun findAllByBandIdAndPaging(
        bandId: UUID,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Performance, UUID> {
        val qPerformance = QPerformance.performance
        val qPerformanceSetlist = QPerformanceSetlist.performanceSetlist
        val qSetlistBand = QSetlistBand.setlistBand

        val contents =
            queryFactory
                .selectFrom(qPerformance)
                .join(qPerformanceSetlist)
                .on(qPerformanceSetlist.performance.eq(qPerformance))
                .join(qSetlistBand)
                .on(qSetlistBand.setlistId.eq(qPerformanceSetlist.setlistId))
                .where(qSetlistBand.bandId.eq(bandId))
                .where(ltPerformanceId(lastId))
                .orderBy(qPerformance.id.desc())
                .distinct()
                .limit(pageSize.toLong() + 1)
                .fetch()

        return buildCursorResponse(contents, pageSize)
    }

    override fun findMyPerformancesByCursor(
        memberId: Long,
        bandIds: List<UUID>,
        lastId: UUID?,
        pageSize: Int,
        from: LocalDate?,
        to: LocalDate?,
    ): CursorResponse<Performance, UUID> {
        val qPerformance = QPerformance.performance
        val qPerformanceSetlist = QPerformanceSetlist.performanceSetlist
        val qSetlistBand = QSetlistBand.setlistBand
        val qPerformanceManager = QPerformanceManager.performanceManager

        // 직접 참여 경로: 공연 멤버(생성자 OWNER · 초대 수락 MANAGER)는 밴드 매핑과 무관하게 항상 포함
        val viaManager =
            JPAExpressions
                .select(qPerformanceManager.performance.id)
                .from(qPerformanceManager)
                .where(qPerformanceManager.member.eq(memberId))
        var condition: BooleanExpression = qPerformance.id.`in`(viaManager)

        // 밴드 참여 경로: 내가 속한 밴드가 셋리스트로 참여하는 공연 — 소속 밴드가 있을 때만 합집합
        if (bandIds.isNotEmpty()) {
            val viaBand =
                JPAExpressions
                    .select(qPerformanceSetlist.performance.id)
                    .from(qPerformanceSetlist)
                    .join(qSetlistBand)
                    .on(qSetlistBand.setlistId.eq(qPerformanceSetlist.setlistId))
                    .where(qSetlistBand.bandId.`in`(bandIds))
            condition = condition.or(qPerformance.id.`in`(viaBand))
        }

        val contents =
            queryFactory
                .selectFrom(qPerformance)
                .where(condition)
                .where(ltPerformanceId(lastId))
                .where(goeStartAt(from))
                .where(ltStartAt(to))
                .orderBy(qPerformance.id.desc())
                .limit(pageSize.toLong() + 1)
                .fetch()

        return buildCursorResponse(contents, pageSize)
    }

    override fun searchByTitleAndPaging(
        keyword: String,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Performance, UUID> {
        val qPerformance = QPerformance.performance

        val contents =
            queryFactory
                .selectFrom(qPerformance)
                .where(qPerformance.title.containsIgnoreCase(keyword))
                .where(ltPerformanceId(lastId))
                .orderBy(qPerformance.id.desc())
                .limit(pageSize.toLong() + 1)
                .fetch()

        return buildCursorResponse(contents, pageSize)
    }

    private fun buildCursorResponse(
        contents: List<Performance>,
        pageSize: Int,
    ): CursorResponse<Performance, UUID> {
        val hasNext = contents.size > pageSize
        val resultContents = if (hasNext) contents.dropLast(1) else contents
        val nextCursor = if (hasNext) resultContents.lastOrNull()?.id else null
        return CursorResponse(content = resultContents, nextCursor = nextCursor, hasNext = hasNext)
    }

    private fun ltPerformanceId(lastId: UUID?): BooleanExpression? = lastId?.let { QPerformance.performance.id.lt(it) }

    private fun goeStartAt(from: LocalDate?): BooleanExpression? =
        from?.let {
            QPerformance.performance.timeInfo.startAt
                .goe(it.atStartOfDay())
        }

    private fun ltStartAt(to: LocalDate?): BooleanExpression? =
        to?.let {
            QPerformance.performance.timeInfo.startAt
                .lt(it.plusDays(1).atStartOfDay())
        }
}
