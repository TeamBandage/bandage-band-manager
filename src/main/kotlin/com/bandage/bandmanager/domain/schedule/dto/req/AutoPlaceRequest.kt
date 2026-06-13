package com.bandage.bandmanager.domain.schedule.dto.req

import com.bandage.bandmanager.domain.schedule.placement.strategy.StrategyPreset
import java.time.LocalDate

/**
 * 자동 배치/미리보기/재배치 요청.
 * preset 기반에 일부 항목을 선택적으로 오버라이드한다. window 미지정 시 보드 window 를 사용한다.
 */
data class AutoPlaceRequest(
    val preset: StrategyPreset = StrategyPreset.BALANCED,
    val windowFrom: LocalDate? = null,
    val windowTo: LocalDate? = null,
    val durationSlots: Int? = null,
    val sessionsPerTrack: Int? = null,
)
