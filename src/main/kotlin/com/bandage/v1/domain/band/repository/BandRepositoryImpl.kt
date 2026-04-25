package com.bandage.v1.domain.band.repository

import com.bandage.v1.domain.band.model.Band
import com.bandage.v1.domain.band.model.QBand
import com.bandage.v1.domain.band.model.QBandMember
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

        val nextCursor = if (hasNext) resultContents.lastOrNull()?.id else null

        return CursorResponse(
            content = resultContents,
            nextCursor = nextCursor,
            hasNext = hasNext,
        )
    }

    override fun findAllByMemberAndPaging(
        memberId: Long,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Band, UUID> {
        val qBand = QBand.band
        val qBandMember = QBandMember.bandMember

        val contents =
            queryFactory
                .selectFrom(qBand)
                .join(qBandMember)
                .on(qBandMember.band.eq(qBand))
                .where(qBandMember.member.eq(memberId))
                .where(ltBandId(lastId))
                .orderBy(qBand.id.desc())
                .distinct()
                .limit(pageSize.toLong() + 1)
                .fetch()

        val hasNext = contents.size > pageSize
        val resultContents = if (hasNext) contents.dropLast(1) else contents
        val nextCursor = if (hasNext) resultContents.lastOrNull()?.id else null

        return CursorResponse(
            content = resultContents,
            nextCursor = nextCursor,
            hasNext = hasNext,
        )
    }

    override fun searchByNameAndPaging(
        keyword: String,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<Band, UUID> {
        val qBand = QBand.band

        val contents =
            queryFactory
                .selectFrom(qBand)
                .where(qBand.name.containsIgnoreCase(keyword))
                .where(ltBandId(lastId))
                .orderBy(qBand.id.desc())
                .limit(pageSize.toLong() + 1)
                .fetch()

        val hasNext = contents.size > pageSize
        val resultContents = if (hasNext) contents.dropLast(1) else contents
        val nextCursor = if (hasNext) resultContents.lastOrNull()?.id else null

        return CursorResponse(
            content = resultContents,
            nextCursor = nextCursor,
            hasNext = hasNext,
        )
    }

    override fun findAllByMemberWithRoleAndPaging(
        memberId: Long,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<BandRepositoryCustom.BandWithRole, UUID> {
        val qBand = QBand.band
        val qBandMember = QBandMember.bandMember

        val tuples =
            queryFactory
                .select(qBand, qBandMember.role)
                .from(qBand)
                .join(qBandMember)
                .on(qBandMember.band.eq(qBand))
                .where(qBandMember.member.eq(memberId))
                .where(ltBandId(lastId))
                .orderBy(qBand.id.desc())
                .limit(pageSize.toLong() + 1)
                .fetch()

        val hasNext = tuples.size > pageSize
        val resultTuples = if (hasNext) tuples.dropLast(1) else tuples
        val resultContents =
            resultTuples.mapNotNull { tuple ->
                val band = tuple.get(qBand) ?: return@mapNotNull null
                val role = tuple.get(qBandMember.role) ?: return@mapNotNull null
                BandRepositoryCustom.BandWithRole(band = band, role = role)
            }
        val nextCursor = if (hasNext) resultContents.lastOrNull()?.band?.id else null

        return CursorResponse(
            content = resultContents,
            nextCursor = nextCursor,
            hasNext = hasNext,
        )
    }

    private fun ltBandId(lastId: UUID?): BooleanExpression? = lastId?.let { QBand.band.id.lt(it) }
}
