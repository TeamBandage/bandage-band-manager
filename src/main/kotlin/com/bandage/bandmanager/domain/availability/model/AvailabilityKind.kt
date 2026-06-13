package com.bandage.bandmanager.domain.availability.model

/**
 * 가용성 예외(AvailabilityException)의 종류.
 * - AVAILABLE: 주간 규칙과 무관하게 해당 시간을 가용으로 표시(추가 가용)
 * - BLOCKED: 주간 규칙으로 가용이더라도 해당 시간을 불가로 표시(차단)
 */
enum class AvailabilityKind {
    AVAILABLE,
    BLOCKED,
}
