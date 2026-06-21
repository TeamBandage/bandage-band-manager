package com.bandage.bandmanager.global.common.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

/**
 * 일정 슬롯. 30분 48슬롯 체계이며 가용 구간은 [startSlot, endSlot) 반열린 구간으로 표현한다.
 * 가용성(MemberAvailability) 조회의 응답 단위이자, 향후 합주/공연 등 멤버 전체 일정을
 * 동일 포맷으로 투영하기 위한 공유 응답 모양이다.
 */
@Schema(description = "일정 슬롯 (30분 48슬롯, [startSlot, endSlot) 반열린 구간)")
data class ScheduleSlotResponse(
    @Schema(description = "날짜", example = "2026-06-13")
    val date: LocalDate,
    @Schema(description = "시작 슬롯 (0~47)", example = "20")
    val startSlot: Int,
    @Schema(description = "종료 슬롯 (1~48, 미포함)", example = "44")
    val endSlot: Int,
    @Schema(description = "슬롯 종류", example = "AVAILABLE")
    val type: ScheduleSlotType,
    @Schema(description = "연결 리소스 ID(합주/공연 등). 가용성 슬롯은 null", nullable = true)
    val refId: String? = null,
    @Schema(description = "표시용 제목. 가용성 슬롯은 null", nullable = true)
    val title: String? = null,
)

// ponytail: 현재 AVAILABLE 단일 값. 통합 일정 피드(JAM/PERFORMANCE 등) 도입 시 여기에 추가.
enum class ScheduleSlotType {
    AVAILABLE,
}
