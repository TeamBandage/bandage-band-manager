package com.bandage.bandmanager.domain.schedule.model

/**
 * ScheduleBlock 의 반복 주기.
 * NONE 이면 단발성 블록, 그 외는 반복 전개(expandRecurrence) 대상이다.
 */
enum class RecurrenceFreq {
    NONE,
    DAILY,
    WEEKLY,
    BIWEEKLY,
}
