package com.bandage.bandmanager.global.authority

import java.time.LocalDateTime

/**
 * 계층형 권한 체계에서 후임 권한자를 선정하는 공통 정책.
 *
 * 상위 우선순위부터 정렬된 후보 그룹들을 받아, 비어있지 않은 첫 그룹에서
 * 가장 오래 전 생성된(createdAt 최소) 후보를 후임으로 반환한다.
 *
 * 예) 밴드: [ADMIN 멤버들, MEMBER 멤버들] → ADMIN 이 있으면 그중 최고참, 없으면 MEMBER 중 최고참
 *     공연: [MANAGER 들, 셋리스트 밴드의 일반 멤버들] → MANAGER 우선, 없으면 밴드 멤버 최고참
 */
object SuccessorSelector {
    fun <T> oldestFromHighestTier(
        tiers: List<List<T>>,
        createdAt: (T) -> LocalDateTime,
    ): T? =
        tiers
            .firstOrNull { it.isNotEmpty() }
            ?.minByOrNull(createdAt)
}
