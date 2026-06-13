package com.bandage.bandmanager.domain.schedule.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.time.LocalDate

/**
 * ScheduleBlock 의 반복 배치 규칙.
 *
 * freq == NONE 이면 단발성이며 나머지 필드는 무시된다.
 * 반복 블록은 ScheduleConfirmFacade.expandRecurrence 에서 [anchorDate, until] 구간을 전개해
 * 실제 Jam 인스턴스들로 펼친다.
 */
@Embeddable
open class RecurrenceRule(
    freq: RecurrenceFreq = RecurrenceFreq.NONE,
    interval: Int = 1,
    until: LocalDate? = null,
    count: Int? = null,
) {
    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_freq", nullable = false)
    var freq: RecurrenceFreq = freq
        protected set

    // freq 의 배수 간격(예: WEEKLY + interval=2 → 격주). 최소 1.
    @Column(name = "recurrence_interval", nullable = false)
    var interval: Int = interval
        protected set

    // 반복 종료일(포함). null 이면 count 로 제한하거나 보드 window 로 제한.
    @Column(name = "recurrence_until", nullable = true)
    var until: LocalDate? = until
        protected set

    // 최대 반복 횟수. null 이면 until/window 로 제한.
    @Column(name = "recurrence_count", nullable = true)
    var count: Int? = count
        protected set

    val isRecurring: Boolean get() = freq != RecurrenceFreq.NONE

    companion object {
        fun none(): RecurrenceRule = RecurrenceRule(freq = RecurrenceFreq.NONE)
    }
}
