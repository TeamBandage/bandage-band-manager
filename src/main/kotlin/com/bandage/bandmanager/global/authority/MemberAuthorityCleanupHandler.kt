package com.bandage.bandmanager.global.authority

/**
 * 회원 탈퇴/삭제 시 해당 회원이 보유한 권한과 소속을 도메인별로 정리하는 포트.
 *
 * 권한 양도 데드락(권한자가 탈퇴하면 누구도 리소스에 접근하지 못하는 현상)을 막기 위해,
 * 회원이 최상위 권한(예: 밴드 LEADER, 공연 OWNER)을 보유한 리소스는 후임자에게 자동 양도하고,
 * 후임 후보가 전혀 없으면 리소스를 소프트 삭제한다.
 *
 * 각 권한 도메인이 이 인터페이스를 구현하면 Spring 이 모든 구현체를
 * [MemberAuthorityCleanupService] 에 List 로 주입하므로, 새 권한 도메인은 구현만 하면 자동 등록된다.
 */
interface MemberAuthorityCleanupHandler {
    /** 이 핸들러가 책임지는 권한 종류 (로깅/식별용). */
    val authorityType: ResourceAuthorityType

    /**
     * [memberId] 가 참여한 이 도메인의 모든 리소스를 정리한다.
     * - 최상위 권한 보유 리소스: 후임자에게 자동 양도, 후임 없으면 리소스 소프트 삭제
     * - 그 외 소속 레코드: 제거
     *
     * 회원 삭제 트랜잭션 내부에서 호출되므로, 실패 시 탈퇴 전체가 롤백된다.
     */
    fun cleanupOnWithdrawal(memberId: Long)
}
