package com.bandage.bandmanager.domain.schedule.dto.res

import java.util.UUID

/**
 * 보드 안에서 셋리스트 트랙이 몇 번 배치됐는지.
 *
 * ScheduleBlockTrack 집계로 산출하므로 수동 배치와 자동 배치가 모두 반영된다.
 * placementCount == 0 이면 아직 한 번도 배치되지 않은 트랙이다.
 */
data class ScheduleBoardPlacementResponse(
    val trackId: UUID,
    val placementCount: Long,
) {
    val placed: Boolean get() = placementCount > 0
}
