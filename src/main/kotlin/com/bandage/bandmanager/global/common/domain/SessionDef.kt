package com.bandage.bandmanager.global.common.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

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
         * 세션 정의 목록을 만든다(BD-229).
         * label 은 검증·대문자화되고, short 는 목록 전체를 보고 서버가 생성한다.
         */
        fun createAll(specs: List<SessionSpec>): List<SessionDef> {
            val normalized = SessionAbbreviationGenerator.normalizeAndGenerate(specs.map { it.label })
            return specs.mapIndexed { index, spec ->
                val (label, short) = normalized[index]
                SessionDef(sessionId = spec.sessionId, label = label, short = short, custom = spec.custom)
            }
        }
    }
}
