package com.bandage.bandmanager.domain.schedule.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 스케줄 블록이 차지하는 시간 구간.
 *
 * 하루는 30분 단위 48슬롯으로 분할되며, 구간은 [startSlot, endSlot) 반열린 구간으로 표현한다.
 * 날짜를 넘기는 구간은 endDate 를 다음 날로 두어 표현한다(예: 23:30~00:30 → endDate = 다음날, endSlot = 1).
 *
 * 슬롯 규약과 검증은 이 클래스가 단독으로 보유하며, ScheduleBlock 은 이를 embed 해서 사용한다.
 */
@Embeddable
data class Slot(
    @Column(name = "start_date", nullable = false)
    val startDate: LocalDate,
    @Column(name = "start_slot", nullable = false)
    val startSlot: Int,
    @Column(name = "end_date", nullable = false)
    val endDate: LocalDate,
    @Column(name = "end_slot", nullable = false)
    val endSlot: Int,
) {
    init {
        validate(startDate, endDate, startSlot, endSlot)
    }

    /** 이 구간의 총 슬롯 수. */
    val totalSlots: Int get() = totalSlots(startDate, endDate, startSlot, endSlot)

    /** 다른 구간과 시간이 겹치는지 여부(반열린 구간 기준, 경계 접촉은 겹침 아님). */
    fun overlaps(other: Slot): Boolean = absoluteStart < other.absoluteEnd && other.absoluteStart < absoluteEnd

    /** epoch day 기준으로 환산한 절대 슬롯 인덱스(구간 비교용). */
    private val absoluteStart: Long get() = startDate.toEpochDay() * SLOTS_PER_DAY + startSlot

    private val absoluteEnd: Long get() = endDate.toEpochDay() * SLOTS_PER_DAY + endSlot

    companion object {
        const val SLOTS_PER_DAY = 48

        fun of(
            startDate: LocalDate,
            startSlot: Int,
            endDate: LocalDate,
            endSlot: Int,
        ): Slot = Slot(startDate, startSlot, endDate, endSlot)

        /** [startDate, startSlot) 부터 [endDate, endSlot) 까지의 총 슬롯 수(exclusive). */
        fun totalSlots(
            startDate: LocalDate,
            endDate: LocalDate,
            startSlot: Int,
            endSlot: Int,
        ): Int {
            val dayDiff = ChronoUnit.DAYS.between(startDate, endDate).toInt()
            return dayDiff * SLOTS_PER_DAY + (endSlot - startSlot)
        }

        fun validate(
            startDate: LocalDate,
            endDate: LocalDate,
            startSlot: Int,
            endSlot: Int,
        ) {
            require(!endDate.isBefore(startDate)) {
                "endDate must be on or after startDate (startDate=$startDate, endDate=$endDate)"
            }
            require(startSlot in 0 until SLOTS_PER_DAY) {
                "startSlot must be in 0..${SLOTS_PER_DAY - 1}, was $startSlot"
            }
            require(endSlot in 0 until SLOTS_PER_DAY) {
                "endSlot must be in 0..${SLOTS_PER_DAY - 1}, was $endSlot"
            }
            require(totalSlots(startDate, endDate, startSlot, endSlot) >= 1) {
                "end must be after start (startDate=$startDate, startSlot=$startSlot, endDate=$endDate, endSlot=$endSlot)"
            }
        }
    }
}
