package com.bandage.bandmanager.domain.schedule.dto.req

import com.bandage.bandmanager.domain.schedule.model.enums.Frequency
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.DayOfWeek

data class ScheduleAutoPlaceRequest(
    val interval: Frequency = Frequency.ONCE,
    @field:Min(1)
    val maxJamsPerDay: Int = 1,
    @field:Max(46)
    @field:Min(0)
    val maxEmptySlotsBetweenJams: Int = 4,
    val dayPreference: List<DayOfWeek> =
        listOf(
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.MONDAY,
        ),
    @field:Max(47)
    @field:Min(0)
    val startTimePreference: Int = 18,
    @field:Max(46)
    @field:Min(0)
    val endTimePreference: Int = 34,
) {
    fun validateTimePreference() {
        if (startTimePreference >= endTimePreference) {
            throw BusinessException(ErrorCode.INVALID_SLOT_RANGE)
        }
    }
}
