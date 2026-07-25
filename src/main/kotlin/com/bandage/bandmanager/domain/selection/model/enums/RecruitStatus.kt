package com.bandage.bandmanager.domain.selection.model.enums

/**
 * 선곡 항목 모집 상태 필터(BD-228).
 *
 * 상호배타적인 "단계"가 아니라 각각 독립적인 조건이며, 목록으로 주면 OR(합집합)로 동작한다.
 * 따라서 상태끼리 겹칠 수 있다: 통상 CLOSED ⊂ ASSIGN_COMPLETED ⊂ APPLY_COMPLETED.
 * 세션이 하나도 없는 항목은 OPEN 으로 정의한다(모집이 시작되지도 않은 상태).
 * 상세: docs/TRACK-SELECTION-FILTER.md
 */
enum class RecruitStatus {
    /** 지원자가 없는 세션이 하나 이상 존재하거나, 세션이 아직 없음 */
    OPEN,

    /** 세션이 1개 이상이고, 모든 세션에 지원자가 1명 이상 */
    APPLY_COMPLETED,

    /** 세션이 1개 이상이고, 모든 세션에 확정자가 1명 이상 */
    ASSIGN_COMPLETED,

    /** 매니저가 선곡을 확정한 항목(isSelected = true) */
    CLOSED,
}
