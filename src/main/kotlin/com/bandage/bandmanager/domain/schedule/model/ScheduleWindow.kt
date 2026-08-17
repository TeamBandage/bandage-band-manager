package com.bandage.bandmanager.domain.schedule.model

import java.time.LocalDate

/**
 * 보드의 배치 가능 날짜 범위 [from, to] (양끝 포함).
 *
 * ScheduleBoard 의 windowFrom/windowTo 두 컬럼을 non-null 쌍으로 승격시켜 주는 값 객체다.
 * 영속 대상이 아니며(ScheduleBoard.scheduleWindowOrNull 이 즉석 생성), 날짜만 표현한다.
 * 하루 안의 시간대는 ScheduleBoard.boardTimeRangeFrom/To 가 담당한다.
 */
data class ScheduleWindow(
    val from: LocalDate,
    val to: LocalDate,
) {
    init {
        require(!from.isAfter(to)) { "scheduleWindow.from must be on or before scheduleWindow.to" }
    }
}
