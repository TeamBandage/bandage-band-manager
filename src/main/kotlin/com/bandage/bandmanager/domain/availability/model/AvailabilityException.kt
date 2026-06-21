package com.bandage.bandmanager.domain.availability.model

import com.bandage.bandmanager.domain.availability.model.WeeklyRule.Companion.SLOTS_PER_DAY
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.time.LocalDate

/**
 * 특정 날짜에 적용되는 가용성 예외.
 *
 * startSlot/endSlot 가 모두 null 이면 해당 날짜 전체(전일)에 적용된다.
 * 일부 구간만 지정할 경우 [startSlot, endSlot) 반열린 구간으로 표현한다.
 */
@Embeddable
open class AvailabilityException(
    date: LocalDate,
    kind: AvailabilityKind,
    startSlot: Int? = null,
    endSlot: Int? = null,
) {
    @Column(name = "exception_date", nullable = false)
    var date: LocalDate = date
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    var kind: AvailabilityKind = kind
        protected set

    @Column(name = "start_slot", nullable = true)
    var startSlot: Int? = startSlot
        protected set

    @Column(name = "end_slot", nullable = true)
    var endSlot: Int? = endSlot
        protected set

    init {
        require((startSlot == null) == (endSlot == null)) {
            "startSlot 과 endSlot 은 함께 지정하거나 함께 비워야 합니다 (전일 적용)."
        }
        if (startSlot != null && endSlot != null) {
            require(startSlot in 0 until SLOTS_PER_DAY) { "startSlot 은 0..${SLOTS_PER_DAY - 1} 범위여야 합니다: $startSlot" }
            require(endSlot in 1..SLOTS_PER_DAY) { "endSlot 은 1..$SLOTS_PER_DAY 범위여야 합니다: $endSlot" }
            require(startSlot < endSlot) { "startSlot($startSlot) 은 endSlot($endSlot) 보다 작아야 합니다." }
        }
    }

    /** 전일 적용 예외인지 여부. */
    val isAllDay: Boolean get() = startSlot == null && endSlot == null

    /** 요청 구간 [reqStart, reqEnd) 와 이 예외가 겹치는지(전일 예외는 항상 겹침). */
    fun overlaps(
        reqStart: Int,
        reqEnd: Int,
    ): Boolean {
        if (isAllDay) return true
        val s = startSlot ?: return true
        val e = endSlot ?: return true
        return s < reqEnd && e > reqStart
    }

    /** 이 예외가 요청 구간 [reqStart, reqEnd) 를 완전히 포함하는지(전일 예외는 항상 포함). */
    fun covers(
        reqStart: Int,
        reqEnd: Int,
    ): Boolean {
        if (isAllDay) return true
        val s = startSlot ?: return true
        val e = endSlot ?: return true
        return s <= reqStart && reqEnd <= e
    }
}
