package com.bandage.bandmanager.domain.schedule.dto.req

import com.bandage.bandmanager.domain.schedule.model.ScheduleBoard
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class ScheduleBoardCreateRequest(
    @field:NotBlank(message = "name 은 필수입니다.")
    @field:Size(max = 50, message = "name 은 최대 50자까지 입력할 수 있습니다.")
    val name: String,
    // 하루 안의 배치 가능 시간대. [from, to) 반열린 구간, 슬롯 인덱스(30분 단위 48슬롯).
    @field:Min(value = 0, message = "boardTimeRangeFrom 은 0 이상이어야 합니다.")
    @field:Max(value = 47, message = "boardTimeRangeFrom 은 47 이하이어야 합니다.")
    val boardTimeRangeFrom: Int = ScheduleBoard.DEFAULT_BOARD_TIME_RANGE_FROM,
    @field:Min(value = 1, message = "boardTimeRangeTo 는 1 이상이어야 합니다.")
    @field:Max(value = 48, message = "boardTimeRangeTo 는 48 이하이어야 합니다.")
    val boardTimeRangeTo: Int = ScheduleBoard.DEFAULT_BOARD_TIME_RANGE_TO,
    // 보드 레벨 연습 가능 날짜 범위(선택). 미지정 시 날짜 검증을 적용하지 않는다.
    val windowFrom: LocalDate? = null,
    val windowTo: LocalDate? = null,
)
