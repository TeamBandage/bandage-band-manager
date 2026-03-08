package com.bandage.v1.domain.auth.handler

import com.bandage.v1.domain.auth.repository.MemberAuthRepository
import org.springframework.stereotype.Component

@Deprecated(message = "회원 인증 로직: 이벤트 기반 아키텍처 적용 X")
@Component
class AuthEventHandler(
    private val memberAuthRepository: MemberAuthRepository,
) {
//    @Async
//    @Transactional
//    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
//    fun handleMemberJoinEvent(event: MemberJoinEvent) {
//        memberAuthRepository.save(
//            MemberAuth.create(
//                memberId = event.memberId,
//                email = event.memberEmail,
//                password = event.memberPassword,
//                role = event.memberRole,
//            ),
//        )
//    }
//
//    @Async
//    @Transactional
//    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
//    fun handleMemberWithdrawnEvent(event: MemberWithdrawnEvent) {
//        val memberAuth =
//            memberAuthRepository.findByMemberId(event.memberId)
//                ?: throw BusinessException(ErrorCode.MEMBER_NOT_FOUND)
//        memberAuth.markAsDeleted()
//        memberAuthRepository.save(memberAuth)
//    }
}
