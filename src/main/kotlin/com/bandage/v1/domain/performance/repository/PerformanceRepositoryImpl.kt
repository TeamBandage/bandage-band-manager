package com.bandage.v1.domain.performance.repository

import com.bandage.v1.domain.performance.model.Performance
import com.bandage.v1.domain.performance.model.QPerformance
import com.bandage.v1.domain.performance.model.QPerformanceBand
import com.bandage.v1.global.common.response.CursorResponse
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import java.util.UUID

class PerformanceRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : PerformanceRepositoryCustom {
    override fun findAllByBandIdAndPaging(
        bandId: UUID,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Performance, UUID> {
        val qPerformance = QPerformance.performance
        val qPerformanceBand = QPerformanceBand.performanceBand

        val contents =
            queryFactory
                .selectFrom(qPerformance)
                .join(qPerformanceBand)
                .on(qPerformanceBand.performance.eq(qPerformance))
                .where(qPerformanceBand.bandId.eq(bandId))
                .where(ltPerformanceId(lastId))
                .orderBy(qPerformance.id.desc())
                .distinct()
                .limit(pageSize.toLong() + 1)
                .fetch()

        val hasNext = contents.size > pageSize
        val resultContents = if (hasNext) contents.dropLast(1) else contents
        val nextCursor = resultContents.lastOrNull()?.id

        return CursorResponse(
            content = resultContents,
            nextCursor = nextCursor,
            hasNext = hasNext,
        )
    }

    private fun ltPerformanceId(lastId: UUID?): BooleanExpression? = lastId?.let { QPerformance.performance.id.lt(it) }
}
