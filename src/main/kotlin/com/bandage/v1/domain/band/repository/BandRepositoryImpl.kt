package com.bandage.v1.domain.band.repository

import com.bandage.v1.domain.band.model.Band
import com.bandage.v1.domain.band.model.QBand
import com.bandage.v1.global.common.response.CursorResponse
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import java.util.UUID

class BandRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : BandRepositoryCustom {
    override fun findAllByPaging(
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Band, UUID> {
        val qBand = QBand.band

        val contents =
            queryFactory
                .selectFrom(qBand)
                .where(ltBandId(lastId))
                .orderBy(qBand.id.desc())
                .limit(pageSize.toLong() + 1) // 실제 요청한 pageSize + 1
                .fetch()

        val hasNext = contents.size > pageSize

        val resultContents = if (hasNext) contents.dropLast(1) else contents // 사이즈 확인 후 마지막 1개 항목 제외 반환

        val nextCursor = resultContents.lastOrNull()?.id

        return CursorResponse(
            content = resultContents,
            nextCursor = nextCursor,
            hasNext = hasNext,
        )
    }

    private fun ltBandId(lastId: UUID?): BooleanExpression? = lastId?.let { QBand.band.id.lt(it) }
}
