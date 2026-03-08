package com.bandage.v1.global.security.handler

import com.bandage.v1.global.security.jwt.RefreshTokenRepository
import org.springframework.stereotype.Component

@Deprecated(message = "회원 인증 로직: 이벤트 기반 아키텍처 적용 X")
@Component
class RefreshTokenEventHandler(
    private val refreshTokenRepository: RefreshTokenRepository,
) {
//    @Async
//    @EventListener
//    fun handleMemberLoginEvent(event: MemberLoginEvent) {
//        refreshTokenRepository.save(
//            memberId = event.memberId,
//            refreshToken = event.refreshToken,
//        )
//    }
//
//    @Async
//    @EventListener
//    fun handleMemberLogoutEvent(event: MemberLogoutEvent) {
//        refreshTokenRepository.delete(event.memberId)
//    }
//
//    @Async
//    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
//    fun handleMemberWithdrawnEvent(event: MemberWithdrawnEvent) {
//        refreshTokenRepository.delete(event.memberId)
//    }
}
