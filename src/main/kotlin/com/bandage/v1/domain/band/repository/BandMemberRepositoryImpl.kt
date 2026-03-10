package com.bandage.v1.domain.band.repository

import com.bandage.v1.domain.band.model.Band
import com.bandage.v1.domain.band.model.BandMember
import com.bandage.v1.domain.band.model.QBandMember
import com.bandage.v1.global.common.response.CursorResponse
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import java.util.UUID

class BandMemberRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : BandMemberRepositoryCustom {
    override fun findAllByPaging(
        lastId: UUID?,
        pageSize: Int,
        band: Band,
    ): CursorResponse<BandMember, UUID> {
        val qBandMember = QBandMember.bandMember

        val contents =
            queryFactory
                .selectFrom(qBandMember)
                .where(qBandMember.band.eq(band))
                .where(ltBandId(lastId))
                .orderBy(qBandMember.id.desc())
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

    private fun ltBandId(lastId: UUID?): BooleanExpression? = lastId?.let { QBandMember.bandMember.id.lt(it) }
}
