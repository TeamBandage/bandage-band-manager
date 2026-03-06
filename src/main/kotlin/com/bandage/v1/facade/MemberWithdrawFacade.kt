package com.bandage.v1.facade

import com.bandage.v1.domain.auth.service.MemberAuthService
import com.bandage.v1.domain.member.service.MemberService
import com.bandage.v1.global.security.jwt.RefreshTokenRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class MemberWithdrawFacade(
    private val memberService: MemberService,
    private val memberAuthService: MemberAuthService,
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    @Transactional
    fun withdrawMember(memberId: Long) {
        memberService.deleteMember(memberId)
        memberAuthService.deleteMemberAuth(memberId)
        refreshTokenRepository.delete(memberId)
    }
}
