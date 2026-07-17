package com.bandage.bandmanager.domain.schedule_bak.placement

import com.bandage.bandmanager.domain.schedule_bak.placement.strategy.StrategyComposition
import java.time.LocalDate
import java.util.UUID

/**
 * 자동 배치 대상 항목. 하나의 트랙을 sessionsNeeded 회 연습하도록 배치한다.
 * memberIds 는 해당 트랙 참여자.
 */
data class PlaceableItem(
    val trackId: UUID,
    val memberIds: Set<Long>,
    val sessionsNeeded: Int,
)

/**
 * AutoPlacer 입력. window, 배치 대상, 전략, 선로딩된 가용성 컨텍스트로 구성된다.
 * availabilityContext 는 전 멤버의 가용성/예약을 한 번에 담아 후보 평가를 in-memory 로 처리한다.
 */
data class PlacementContext(
    val windowFrom: LocalDate,
    val windowTo: LocalDate,
    val items: List<PlaceableItem>,
    val strategy: StrategyComposition,
    val availabilityContext: AvailabilityContext,
    // replan 시 고정(anchor/pinned)된 블록. 새 배치가 이들과 겹치지 않도록 pending 으로 선점한다.
    val anchored: List<PendingBlock> = emptyList(),
)
