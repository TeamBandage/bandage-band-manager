package com.bandage.bandmanager.domain.schedule.model.enums

/**
 * 자동배치의 회차 주기.
 *
 * 스케줄 보드 윈도우를 이 주기로 쪼갠 각 구간이 1회차이며, 각 잼 그룹은 회차당 최대 1회 배치된다.
 * 경계는 고정 길이가 아니라 **달력** 기준이다. 사용자가 "9월엔 네 번"처럼 달력으로 이해하는 단위와 맞추기 위함이다.
 *
 * - ONCE: 윈도우 전체가 1회차
 * - DAILY: 각 날짜
 * - WEEKLY: 월요일 시작 주(ISO 8601)
 * - BIWEEKLY: windowFrom 이 속한 주부터 2주씩
 *   (ISO 주번호의 짝/홀로 나누면 윈도우와 무관하게 경계가 정해져 사용자가 이유를 알 수 없다)
 * - MONTHLY: 달력 월(1일~말일)
 *
 * 윈도우 양 끝에 생기는 부분 회차(예: 목~일 4일짜리 첫 주)도 1회차로 인정한다.
 * 짧아서 배치할 슬롯이 없으면 그 회차만 실패하고 다음 회차로 넘어간다.
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
