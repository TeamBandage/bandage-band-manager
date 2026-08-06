package com.bandage.bandmanager.global.common.domain

import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.util.UUID

@Embeddable
open class SessionDef(
    sessionId: String,
    label: String,
    short: String,
    custom: Boolean,
) {
    @Column(name = "session_id", nullable = false)
    var sessionId: String = sessionId
        protected set

    @Column(name = "label", nullable = false)
    var label: String = label
        protected set

    @Column(name = "session_short", nullable = false)
    var short: String = short
        protected set

    @Column(name = "session_custom", nullable = false)
    var custom: Boolean = custom
        protected set

    companion object {
        /**
         * 세션 정의 목록을 만든다(BD-229, BD-269).
         *
         * label 은 검증·대문자화되고, short 는 목록 전체를 보고 서버가 생성한다.
         * sessionId 는 배정(지원/확정/참여자)이 참조하는 키이므로 서버가 발급한다 — spec 의 sessionId 가
         * null 이면 새로 발급하고, 값이 있으면 [existingSessionIds] 에 실존하는지 검증한다.
         *
         * @param existingSessionIds 교체 대상 목록의 현재 sessionId 집합. 신규 생성 시에는 비어 있다.
         * @throws BusinessException 알 수 없는 sessionId 를 지정했거나(SESSION_NOT_FOUND) 같은 값을 중복 지정한 경우(SESSION_DUPLICATED)
         */
        fun createAll(
            specs: List<SessionSpec>,
            existingSessionIds: Set<String> = emptySet(),
        ): List<SessionDef> {
            val normalized = SessionAbbreviationGenerator.normalizeAndGenerate(specs.map { it.label })
            val seen = mutableSetOf<String>()
            return specs.mapIndexed { index, spec ->
                val (label, short) = normalized[index]
                val sessionId = spec.sessionId ?: UUID.randomUUID().toString()
                if (spec.sessionId != null && spec.sessionId !in existingSessionIds) {
                    throw BusinessException(ErrorCode.SESSION_NOT_FOUND)
                }
                if (!seen.add(sessionId)) {
                    throw BusinessException(ErrorCode.SESSION_DUPLICATED)
                }
                SessionDef(sessionId = sessionId, label = label, short = short, custom = spec.custom)
            }
        }
    }
}
