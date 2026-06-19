package com.bandage.bandmanager.global.authority

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 회원 탈퇴 시 등록된 모든 [MemberAuthorityCleanupHandler] 를 순회 호출하는 오케스트레이터.
 *
 * 회원 삭제와 동일한 트랜잭션에서 권한 양도/정리를 수행하기 위해, 회원 삭제 이전에 호출한다.
 * 핸들러가 비어 있어도(권한 도메인 미존재) 무해하게 동작한다.
 */
@Service
class MemberAuthorityCleanupService(
    private val handlers: List<MemberAuthorityCleanupHandler>,
) {
    @Transactional
    fun cleanupAll(memberId: Long) {
        handlers.forEach { it.cleanupOnWithdrawal(memberId) }
    }
}
