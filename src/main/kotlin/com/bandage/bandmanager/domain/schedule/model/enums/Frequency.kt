package com.bandage.bandmanager.domain.schedule.model.enums

/**
 * 자동배치의 회차 주기.
 *
 * 스케줄 보드 윈도우를 이 주기로 쪼갠 각 구간이 1회차이며, 각 잼 그룹은 회차당 최대 1회 배치된다.
 * ONCE 면 윈도우 전체가 1회차다.
 *
 * 블록에 반복 규칙을 붙여 나중에 전개하는 방식(RecurrenceRule)은 사용하지 않는다.
 * 회차 수만큼 블록을 실제로 생성하므로, 어떤 회차가 배치에 실패해도 나머지 회차는 그대로 남는다.
 */
enum class Frequency {
    ONCE,
    DAILY,
    WEEKLY,
    BIWEEKLY,
    MONTHLY,
}
