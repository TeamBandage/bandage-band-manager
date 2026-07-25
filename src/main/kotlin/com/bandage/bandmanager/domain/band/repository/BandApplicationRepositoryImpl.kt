package com.bandage.bandmanager.domain.band.repository

import com.bandage.bandmanager.domain.band.model.Band
import com.bandage.bandmanager.domain.band.model.BandApplication
import com.bandage.bandmanager.domain.band.model.QBandApplication
import com.bandage.bandmanager.domain.band.model.enums.ApplicationStatus
import com.bandage.bandmanager.global.common.response.CursorResponse
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import java.util.UUID

class BandApplicationRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : BandApplicationRepositoryCustom {
    override fun findAllByPaging(
        lastId: UUID?,
        pageSize: Int,
        status: ApplicationStatus,
        band: Band,
    ): CursorResponse<BandApplication, UUID> {
        val qBandApplication = QBandApplication.bandApplication

        val contents =
            queryFactory
                .selectFrom(qBandApplication)
                .where(qBandApplication.band.eq(band))
                .where(qBandApplication.status.eq(status))
                // BD-227: 한 회원당 최신 신청서 1건만 노출한다(나의 신청서 조회와 동일 기준).
                .where(qBandApplication.isLatest.isTrue)
                .where(ltBandId(lastId))
                .orderBy(qBandApplication.id.desc())
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

    override fun findAllByMemberPaging(
        lastId: UUID?,
        pageSize: Int,
        status: ApplicationStatus?,
        memberId: Long,
    ): CursorResponse<BandApplication, UUID> {
        val qBandApplication = QBandApplication.bandApplication

        val contents =
            queryFactory
                .selectFrom(qBandApplication)
                .join(qBandApplication.band)
                .fetchJoin()
                .where(qBandApplication.member.eq(memberId))
                .where(qBandApplication.isLatest.isTrue)
                .where(status?.let { qBandApplication.status.eq(it) })
                .where(ltBandId(lastId))
                .orderBy(qBandApplication.id.desc())
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

    private fun ltBandId(lastId: UUID?): BooleanExpression? = lastId?.let { QBandApplication.bandApplication.id.lt(it) }
}
