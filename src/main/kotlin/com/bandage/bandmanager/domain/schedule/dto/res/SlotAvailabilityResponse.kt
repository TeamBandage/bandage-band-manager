package com.bandage.bandmanager.domain.schedule.dto.res

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

/**
 * 슬롯 한 칸에 대한 셋리스트 참여 멤버들의 가용 여부.
 *
 * 클라이언트가 기간 단위로 벌크 조회해 캐싱한 뒤, 슬롯 호버링 시 추가 호출 없이 표시하는 용도다.
 * 특정 트랙을 배치하려는 경우, 그 트랙의 참여자 목록과 availableMemberIds 를 교집합하면
 * 해당 슬롯에서 누가 가능하고 누가 불가능한지 클라이언트에서 바로 판정할 수 있다.
 */
@Schema(description = "슬롯별 멤버 가용 현황")
data class SlotAvailabilityResponse(
    @Schema(description = "날짜", example = "2026-09-01")
    val date: LocalDate,
    @Schema(description = "슬롯 인덱스 (0~47, 30분 단위)", example = "36")
    val slot: Int,
    @Schema(description = "이 슬롯에 가용한 멤버 ID")
    val availableMemberIds: List<Long>,
    @Schema(description = "이 슬롯에 불가능한 멤버 ID")
    val unavailableMemberIds: List<Long>,
)
