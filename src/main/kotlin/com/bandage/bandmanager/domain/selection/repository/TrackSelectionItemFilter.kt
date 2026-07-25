package com.bandage.bandmanager.domain.selection.repository

import com.bandage.bandmanager.domain.selection.model.enums.RecruitStatus
import com.bandage.bandmanager.domain.selection.model.enums.TrackSearchField

/**
 * 선곡 항목 목록 필터 조건(BD-228).
 *
 * f1~f4 는 서로 독립적이며 AND 로 결합된다. 각 필드가 null/빈 값이면 해당 필터는 적용되지 않는다.
 * 웹 계층 DTO 대신 이 캐리어를 쓰는 이유는 리포지토리가 요청 DTO에 의존하지 않게 하기 위함이다.
 */
data class TrackSelectionItemFilter(
    val status: List<RecruitStatus>? = null,
    val appliedByMe: Boolean? = null,
    val memberName: String? = null,
    val keyword: String? = null,
    val searchFields: List<TrackSearchField>? = null,
)
