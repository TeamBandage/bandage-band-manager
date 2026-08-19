package com.bandage.bandmanager.domain.performance.repository

import com.bandage.bandmanager.domain.performance.model.Performance
import com.bandage.bandmanager.domain.performance.model.PerformanceInvitation
import com.bandage.bandmanager.domain.performance.model.QPerformanceInvitation
import com.bandage.bandmanager.domain.performance.model.enums.PerformanceInvitationStatus
import com.bandage.bandmanager.global.common.response.CursorResponse
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import java.util.UUID

/**
 * 초대 목록 커서 페이징(BD-286).
 *
 * PK 가 UUIDv7 이라 id 내림차순이 생성 시각 내림차순과 같다. 커서 비교가 가능한 id 를 정렬 키로 쓴다
 * (기존 createdAt 정렬은 커서로 쓸 수 없어 id 로 대체했고, 노출 순서는 동일하다).
 */
class PerformanceInvitationRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : PerformanceInvitationRepositoryCustom {
    override fun findAllByPerformanceAndPaging(
        performance: Performance,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<PerformanceInvitation, UUID> =
        fetchPage(QPerformanceInvitation.performanceInvitation.performance.eq(performance), lastId, pageSize)

    override fun findAllByInvitedMemberAndStatusAndPaging(
        invitedMember: Long,
        status: PerformanceInvitationStatus,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<PerformanceInvitation, UUID> {
        val qInvitation = QPerformanceInvitation.performanceInvitation
        return fetchPage(
            qInvitation.invitedMember.eq(invitedMember).and(qInvitation.status.eq(status)),
            lastId,
            pageSize,
        )
    }

    private fun fetchPage(
        condition: BooleanExpression,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<PerformanceInvitation, UUID> {
        val qInvitation = QPerformanceInvitation.performanceInvitation

        val rows =
            queryFactory
                .selectFrom(qInvitation)
                .where(condition)
                .where(ltInvitationId(lastId))
                .orderBy(qInvitation.id.desc())
                .limit(pageSize.toLong() + 1)
                .fetch()

        return CursorResponse.of(rows, pageSize) { it.id }
    }

    private fun ltInvitationId(lastId: UUID?): BooleanExpression? = lastId?.let { QPerformanceInvitation.performanceInvitation.id.lt(it) }
}
