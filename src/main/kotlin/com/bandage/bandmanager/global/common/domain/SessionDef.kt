package com.bandage.bandmanager.global.common.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
open class SessionDef(
    sessionId: String,
    label: String,
    short: String,
    need: Int,
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

    @Column(name = "session_need", nullable = false)
    var need: Int = need
        protected set

    @Column(name = "session_custom", nullable = false)
    var custom: Boolean = custom
        protected set
}
