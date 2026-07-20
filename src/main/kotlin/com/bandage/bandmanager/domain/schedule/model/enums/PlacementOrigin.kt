package com.bandage.bandmanager.domain.schedule.model.enums

/**
 * ScheduleBlock 이 어떻게 배치되었는지 출처.
 * - MANUAL: 사용자가 직접 배치
 * - AUTO: AutoPlacer 자동 배치
 * - ANCHORED: 자동 배치 후 사용자가 고정(anchor)한 블록
 */
enum class PlacementOrigin {
    MANUAL,
    AUTO,
    ANCHORED,
}
