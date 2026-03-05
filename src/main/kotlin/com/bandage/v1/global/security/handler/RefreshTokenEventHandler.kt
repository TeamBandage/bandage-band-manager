package com.bandage.v1.global.security.handler

import com.bandage.v1.global.async.event.MemberLoginEvent
import com.bandage.v1.global.async.event.MemberLogoutEvent
import com.bandage.v1.global.async.event.MemberWithdrawnEvent
import com.bandage.v1.global.security.jwt.RefreshTokenService
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class RefreshTokenEventHandler(
    private val refreshTokenService: RefreshTokenService,
) {
    @Async
    @EventListener
    fun handleMemberLoginEvent(event: MemberLoginEvent) {
        refreshTokenService.saveRefreshToken(
            memberId = event.memberId,
            refreshToken = event.refreshToken,
        )
    }

    @Async
    @EventListener
    fun handleMemberLogoutEvent(event: MemberLogoutEvent) {
        refreshTokenService.deleteRefreshToken(event.memberId)
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleMemberWithdrawnEvent(event: MemberWithdrawnEvent) {
        refreshTokenService.deleteRefreshToken(event.memberId)
    }
}
