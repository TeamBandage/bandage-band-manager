package com.bandage.bandmanager.global.authority

/**
 * 회원 이탈 시 자동 양도 대상이 되는 '최상위 권한(소유권)'의 종류.
 * 새로운 권한 도메인을 추가할 때 항목을 확장한다.
 */
enum class ResourceAuthorityType {
    BAND_LEADERSHIP,
    PERFORMANCE_OWNERSHIP,
    SETLIST_MANAGEMENT,
    TRACK_SELECTION_MANAGEMENT,
}
