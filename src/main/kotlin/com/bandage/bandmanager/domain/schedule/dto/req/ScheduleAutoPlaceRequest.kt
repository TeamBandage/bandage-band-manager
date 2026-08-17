package com.bandage.bandmanager.domain.schedule.dto.req

import com.bandage.bandmanager.domain.schedule.model.enums.Frequency
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.DayOfWeek

/**
 * 자동 배치 요청.
 *
 * 시간 관련 필드는 모두 슬롯 인덱스(하루 30분 단위 48슬롯)다. 시(hour)가 아니다.
 * 시간대 구간 [startTimePreference, endTimePreference) 는 반열린 구간이므로 end 는 1..48 이다.
 */
data class ScheduleAutoPlaceRequest(
    val interval: Frequency = Frequency.ONCE,
    // 잼 1곡당 배치 길이(슬롯). 그룹에 곡이 여러 개면 이 값 × 곡 수 만큼의 연속 구간을 잡는다.
    @field:Min(1)
    @field:Max(48)
    val jamDurationSlots: Int = DEFAULT_JAM_DURATION_SLOTS,
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
    // 반열린 구간의 끝이므로 1..48. 48 이면 당일 24:00 을 뜻한다.
    @field:Max(48)
    @field:Min(1)
    val endTimePreference: Int = 34,
) {
    fun validateTimePreference() {
        if (startTimePreference >= endTimePreference) {
            throw BusinessException(ErrorCode.INVALID_SLOT_RANGE)
        }
    }

    companion object {
        /** 기본 잼 길이: 4슬롯 = 2시간. */
        const val DEFAULT_JAM_DURATION_SLOTS = 4
    }
}
