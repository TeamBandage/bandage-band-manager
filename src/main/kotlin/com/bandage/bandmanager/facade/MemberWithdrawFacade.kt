package com.bandage.bandmanager.facade

import com.bandage.bandmanager.domain.auth.service.MemberAuthService
import com.bandage.bandmanager.domain.member.service.MemberService
import com.bandage.bandmanager.global.authority.MemberAuthorityCleanupService
import com.bandage.bandmanager.global.security.jwt.RefreshTokenRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class MemberWithdrawFacade(
    private val memberService: MemberService,
    private val memberAuthService: MemberAuthService,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val memberAuthorityCleanupService: MemberAuthorityCleanupService,
) {
    @Transactional
    fun withdrawMember(memberId: Long) {
        // 회원 삭제 전, 보유 권한을 후임자에게 자동 양도/정리해 데드락을 방지한다(동일 트랜잭션).
        memberAuthorityCleanupService.cleanupAll(memberId)
        memberService.deleteMember(memberId)
        memberAuthService.deleteMemberAuth(memberId)
        refreshTokenRepository.delete(memberId)
    }
}
