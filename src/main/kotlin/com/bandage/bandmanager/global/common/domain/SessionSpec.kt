package com.bandage.bandmanager.global.common.domain

/**
 * 세션 정의 입력값(BD-229).
 *
 * 약어(short)는 개별 세션이 아니라 세션 목록 단위로 서버가 생성하므로 입력에 포함하지 않는다.
 * label 검증/정규화와 약어 생성은 [SessionDef.createAll] 이 담당한다.
 */
data class SessionSpec(
    val sessionId: String,
    val label: String,
    val custom: Boolean = false,
)
