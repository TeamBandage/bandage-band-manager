package com.bandage.bandmanager.facade

import com.bandage.bandmanager.domain.auth.service.MemberAuthService
import com.bandage.bandmanager.domain.member.service.MemberService
import com.bandage.bandmanager.facade.dto.MemberJoinRequest
import com.bandage.bandmanager.facade.dto.MemberResponse
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class MemberJoinFacade(
    private val memberService: MemberService,
    private val memberAuthService: MemberAuthService,
) {
    @Transactional
    fun joinMember(request: MemberJoinRequest): MemberResponse {
        val member = memberService.createMember(request.toMemberCreateRequest())
        memberAuthService.createMemberAuth(request.toMemberAuthCreateRequest(member.id))
        return MemberResponse.of(member)
    }
}
