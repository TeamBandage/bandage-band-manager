package com.bandage.bandmanager.domain.selection.repository

import com.bandage.bandmanager.domain.member.model.QMember
import com.bandage.bandmanager.domain.selection.model.QTrackSelectionItem
import com.bandage.bandmanager.domain.selection.model.QTrackSelectionItemApplicant
import com.bandage.bandmanager.domain.selection.model.QTrackSelectionItemConfirmation
import com.bandage.bandmanager.domain.selection.model.enums.RecruitStatus
import com.bandage.bandmanager.domain.selection.model.enums.TrackSearchField
import com.bandage.bandmanager.global.common.domain.QSessionDef
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.JPAExpressions

/**
 * 선곡 항목 목록 필터 술어(BD-228).
 *
 * 설계 원칙
 * - 모든 필터는 커서 쿼리의 WHERE 로 내린다. 메모리 후필터링은 `limit pageSize+1` 기반
 *   hasNext/nextCursor 계산을 깨뜨린다(요청 50건에 3건 반환 + hasNext=true, 커서가 행 건너뜀).
 * - 세션(@ElementCollection)은 outer join 하지 않고 상관 서브쿼리로만 참조한다.
 *   join 하면 세션 수만큼 행이 불어나 limit/커서가 어긋난다.
 * - null/빈 입력은 null 을 반환해 QueryDSL `where(null)` = 무조건 참 규약에 맡긴다.
 *
 * 상세 명세: docs/TRACK-SELECTION-FILTER.md
 */
internal object TrackSelectionItemFilterPredicates {
    private val item = QTrackSelectionItem.trackSelectionItem

    // -------- f1: 모집 상태 --------

    /** status 목록은 OR 로 합집합. 상태끼리 겹치는 것을 의도적으로 허용한다. */
    fun statusIn(statuses: List<RecruitStatus>?): BooleanExpression? {
        if (statuses.isNullOrEmpty()) return null
        return statuses
            .distinct()
            .map(::statusPredicate)
            .reduce { acc, next -> acc.or(next) }
    }

    private fun statusPredicate(status: RecruitStatus): BooleanExpression =
        when (status) {
            RecruitStatus.OPEN -> hasSessionWithoutApplicant().or(hasSession().not())
            RecruitStatus.APPLY_COMPLETED -> hasSession().and(hasSessionWithoutApplicant().not())
            RecruitStatus.ASSIGN_COMPLETED -> hasSession().and(hasSessionWithoutConfirmation().not())
            RecruitStatus.CLOSED -> item.isSelected.isTrue()
        }

    /**
     * 세션 0개 항목에서 "모든 세션이 충족" 이 공허하게 참이 되는 것을 막는 가드.
     * APPLY/ASSIGN_COMPLETED 는 반드시 이 조건과 AND 로 묶는다.
     */
    private fun hasSession(): BooleanExpression =
        JPAExpressions
            .selectOne()
            .from(item._sessions, QSessionDef.sessionDef)
            .exists()

    /**
     * "지원자가 없는 세션이 하나라도 존재" — 이중 부정으로 전칭(∀)을 표현한다.
     * ∀session ∃applicant ≡ ¬∃session ¬∃applicant
     * count(distinct) 비교 대신 이 형태를 택한 이유: 첫 반증 세션에서 단락 평가되고,
     * 세션 교체 후 남은 고아 session_id 가 카운트를 부풀려 오탐하는 문제가 없다.
     */
    private fun hasSessionWithoutApplicant(): BooleanExpression {
        val session = QSessionDef.sessionDef
        val applicant = QTrackSelectionItemApplicant.trackSelectionItemApplicant
        return JPAExpressions
            .selectOne()
            .from(item._sessions, session)
            .where(
                JPAExpressions
                    .selectOne()
                    .from(applicant)
                    .where(
                        applicant.item.eq(item),
                        applicant.sessionId.eq(session.sessionId),
                    ).notExists(),
            ).exists()
    }

    private fun hasSessionWithoutConfirmation(): BooleanExpression {
        val session = QSessionDef.sessionDef
        val confirmation = QTrackSelectionItemConfirmation.trackSelectionItemConfirmation
        return JPAExpressions
            .selectOne()
            .from(item._sessions, session)
            .where(
                JPAExpressions
                    .selectOne()
                    .from(confirmation)
                    .where(
                        confirmation.item.eq(item),
                        confirmation.sessionId.eq(session.sessionId),
                    ).notExists(),
            ).exists()
    }

    // -------- f2: 내가 지원한 항목 --------

    /** true=내가 지원한 항목만, false=내가 지원하지 않은 항목만, null=무필터. */
    fun appliedByMe(
        appliedByMe: Boolean?,
        memberId: Long,
    ): BooleanExpression? {
        if (appliedByMe == null) return null
        val applicant = QTrackSelectionItemApplicant.trackSelectionItemApplicant
        val exists =
            JPAExpressions
                .selectOne()
                .from(applicant)
                .where(
                    applicant.item.eq(item),
                    applicant.memberId.eq(memberId),
                ).exists()
        return if (appliedByMe) exists else exists.not()
    }

    // -------- f3: 참여자 이름 검색 --------

    /**
     * "참여자" = 지원자(applicant). 확정자는 정상 흐름상 지원자의 부분집합이므로
     * 서브쿼리 비용을 추가하지 않는다(근거: docs/TRACK-SELECTION-FILTER.md).
     * Member 는 @SQLRestriction 대상이라 탈퇴 회원은 자동 제외된다.
     */
    fun memberNameContains(memberName: String?): BooleanExpression? {
        val keyword = memberName?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val applicant = QTrackSelectionItemApplicant.trackSelectionItemApplicant
        val member = QMember.member
        return JPAExpressions
            .selectOne()
            .from(applicant)
            .join(member)
            .on(member.id.eq(applicant.memberId))
            .where(
                applicant.item.eq(item),
                member.name.containsIgnoreCase(keyword),
            ).exists()
    }

    // -------- f4: 트랙 정보 검색 --------

    /** searchFields 미지정 시 TITLE/ARTIST/ALBUM 전체 OR 검색. album 은 nullable 이라 NULL 은 미매치. */
    fun trackInfoContains(
        keyword: String?,
        searchFields: List<TrackSearchField>?,
    ): BooleanExpression? {
        val query = keyword?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val fields = searchFields?.distinct()?.takeIf { it.isNotEmpty() } ?: TrackSearchField.entries
        return fields
            .map { field ->
                when (field) {
                    TrackSearchField.TITLE -> item.trackInfo.title.containsIgnoreCase(query)
                    TrackSearchField.ARTIST -> item.trackInfo.artist.containsIgnoreCase(query)
                    TrackSearchField.ALBUM -> item.trackInfo.album.containsIgnoreCase(query)
                }
            }.reduce { acc, next -> acc.or(next) }
    }
}
