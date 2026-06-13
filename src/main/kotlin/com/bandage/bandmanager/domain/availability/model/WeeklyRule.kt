package com.bandage.bandmanager.domain.availability.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 주간 반복 가용 규칙.
 *
 * 하루는 30분 단위 48슬롯으로 분할되며, 가용 구간은 [startSlot, endSlot) 반열린 구간으로 표현한다.
 * (startSlot ∈ 0..47, endSlot ∈ 1..48, startSlot < endSlot). ScheduleBlock 의 슬롯 체계와 정합한다.
 *
 * effectiveFrom/effectiveTo 로 규칙의 유효 기간을 한정할 수 있으며, effectiveTo == null 이면 무기한이다.
 */
@Embeddable
open class WeeklyRule(
    dayOfWeek: DayOfWeek,
    startSlot: Int,
    endSlot: Int,
    effectiveFrom: LocalDate,
    effectiveTo: LocalDate? = null,
) {
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    var dayOfWeek: DayOfWeek = dayOfWeek
        protected set

    @Column(name = "start_slot", nullable = false)
    var startSlot: Int = startSlot
        protected set

    @Column(name = "end_slot", nullable = false)
    var endSlot: Int = endSlot
        protected set

    @Column(name = "effective_from", nullable = false)
    var effectiveFrom: LocalDate = effectiveFrom
        protected set

    @Column(name = "effective_to", nullable = true)
    var effectiveTo: LocalDate? = effectiveTo
        protected set

    init {
        require(startSlot in 0 until SLOTS_PER_DAY) { "startSlot 은 0..${SLOTS_PER_DAY - 1} 범위여야 합니다: $startSlot" }
        require(endSlot in 1..SLOTS_PER_DAY) { "endSlot 은 1..$SLOTS_PER_DAY 범위여야 합니다: $endSlot" }
        require(startSlot < endSlot) { "startSlot($startSlot) 은 endSlot($endSlot) 보다 작아야 합니다." }
        if (effectiveTo != null) {
            require(!effectiveTo.isBefore(effectiveFrom)) { "effectiveTo($effectiveTo) 는 effectiveFrom($effectiveFrom) 이후여야 합니다." }
        }
    }

    /** 주어진 날짜에 이 규칙이 유효한지 여부. */
    fun isEffectiveOn(date: LocalDate): Boolean {
        if (date.isBefore(effectiveFrom)) return false
        if (effectiveTo != null && date.isAfter(effectiveTo)) return false
        return date.dayOfWeek == dayOfWeek
    }

    companion object {
        const val SLOTS_PER_DAY: Int = 48
    }
}
