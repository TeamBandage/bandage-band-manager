package com.bandage.v1.domain.auth.handler

import com.bandage.v1.domain.auth.model.MemberAuth
import com.bandage.v1.domain.auth.repository.MemberAuthRepository
import com.bandage.v1.global.async.event.MemberJoinEvent
import com.bandage.v1.global.async.event.MemberWithdrawnEvent
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import jakarta.transaction.Transactional
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AuthEventHandler(
    private val memberAuthRepository: MemberAuthRepository,
) {
    @Async
    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleMemberJoinEvent(event: MemberJoinEvent) {
        memberAuthRepository.save(
            MemberAuth.create(
                memberId = event.memberId,
                email = event.memberEmail,
                password = event.memberPassword,
                role = event.memberRole,
            ),
        )
    }

    @Async
    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleMemberWithdrawnEvent(event: MemberWithdrawnEvent) {
        val memberAuth =
            memberAuthRepository.findByMemberId(event.memberId)
                ?: throw BusinessException(ErrorCode.MEMBER_NOT_FOUND)
        memberAuth.markAsDeleted()
        memberAuthRepository.save(memberAuth)
    }
}
