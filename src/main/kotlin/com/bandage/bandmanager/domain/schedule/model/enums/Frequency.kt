package com.bandage.bandmanager.domain.schedule.model.enums

/**
 * ScheduleBlock 의 반복 주기.
 * ONCE 이면 단발성 블록, 그 외는 반복 전개(expandRecurrence) 대상
 */
enum class Frequency {
    ONCE,
    DAILY,
    WEEKLY,
    BIWEEKLY,
    MONTHLY,
}
