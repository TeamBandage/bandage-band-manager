package com.bandage.bandmanager.domain.schedule.placement.strategy

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 후보 날짜 생성 전략. 보드 window([from, to]) 내에서 배치 후보 날짜를 생성한다.
 * Jackson 다형성으로 직렬화되며 type 필드로 구분한다.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = DateGenerator.AllDays::class, name = "ALL_DAYS"),
    JsonSubTypes.Type(value = DateGenerator.SpecificWeekdays::class, name = "SPECIFIC_WEEKDAYS"),
    JsonSubTypes.Type(value = DateGenerator.IntervalDays::class, name = "INTERVAL_DAYS"),
)
sealed interface DateGenerator {
    fun generate(
        from: LocalDate,
        to: LocalDate,
    ): List<LocalDate>

    /** window 내 모든 날짜. */
    data class AllDays(
        val unused: Boolean = true,
    ) : DateGenerator {
        override fun generate(
            from: LocalDate,
            to: LocalDate,
        ): List<LocalDate> = datesBetween(from, to)
    }

    /** 지정 요일만. */
    data class SpecificWeekdays(
        val days: Set<DayOfWeek>,
    ) : DateGenerator {
        override fun generate(
            from: LocalDate,
            to: LocalDate,
        ): List<LocalDate> = datesBetween(from, to).filter { it.dayOfWeek in days }
    }

    /** from 부터 stepDays 간격. */
    data class IntervalDays(
        val stepDays: Int,
    ) : DateGenerator {
        override fun generate(
            from: LocalDate,
            to: LocalDate,
        ): List<LocalDate> {
            val step = stepDays.coerceAtLeast(1).toLong()
            val result = mutableListOf<LocalDate>()
            var cur = from
            while (!cur.isAfter(to)) {
                result.add(cur)
                cur = cur.plusDays(step)
            }
            return result
        }
    }

    companion object {
        internal fun datesBetween(
            from: LocalDate,
            to: LocalDate,
        ): List<LocalDate> {
            if (from.isAfter(to)) return emptyList()
            val result = mutableListOf<LocalDate>()
            var cur = from
            while (!cur.isAfter(to)) {
                result.add(cur)
                cur = cur.plusDays(1)
            }
            return result
        }
    }
}
