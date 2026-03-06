package com.bandage.v1.facade

import com.bandage.v1.domain.auth.dto.req.MemberAuthCreateRequest
import com.bandage.v1.domain.auth.service.MemberAuthService
import com.bandage.v1.domain.member.service.MemberService
import com.bandage.v1.facade.dto.MemberJoinRequest
import com.bandage.v1.facade.dto.MemberResponse
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
        val authRequest =
            MemberAuthCreateRequest(
                memberId = member.id!!,
                email = member.email,
                password = member.password,
                role = member.role,
            )
        memberAuthService.createMemberAuth(authRequest)
        return MemberResponse.of(member)
    }
}
