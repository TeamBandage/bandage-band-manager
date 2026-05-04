package com.bandage.v1.facade

import com.bandage.v1.domain.auth.model.MemberAuth
import com.bandage.v1.domain.auth.model.enums.ProviderType
import com.bandage.v1.domain.auth.oauth.OAuthUserInfo
import com.bandage.v1.domain.auth.service.MemberAuthService
import com.bandage.v1.domain.member.model.Member
import com.bandage.v1.domain.member.repository.MemberRepository
import com.bandage.v1.facade.dto.OAuthLoginResponse
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OAuthLoginFacade(
    private val memberRepository: MemberRepository,
    private val memberAuthService: MemberAuthService,
) {
    @Transactional
    fun loginOrJoin(userInfo: OAuthUserInfo): OAuthLoginResponse {
        val existingAuth = memberAuthService.findByEmail(userInfo.email)
        return if (existingAuth != null) {
            verifyProviderMatch(existingAuth, userInfo.provider)
            val tokens = memberAuthService.issueTokens(existingAuth)
            OAuthLoginResponse.of(tokens, isNewMember = false)
        } else {
            val newAuth = registerOAuthMember(userInfo)
            val tokens = memberAuthService.issueTokens(newAuth)
            OAuthLoginResponse.of(tokens, isNewMember = true)
        }
    }

    private fun verifyProviderMatch(
        memberAuth: MemberAuth,
        provider: ProviderType,
    ) {
        if (memberAuth.provider != provider) {
            throw BusinessException(ErrorCode.OAUTH_PROVIDER_MISMATCH)
        }
    }

    private fun registerOAuthMember(userInfo: OAuthUserInfo): MemberAuth {
        val member =
            memberRepository.save(
                Member.createOAuth(
                    email = userInfo.email,
                    name = userInfo.name,
                    profileImg = userInfo.profileImg,
                ),
            )
        return memberAuthService.createOAuthMemberAuth(
            memberId = member.id,
            email = member.email,
            provider = userInfo.provider,
            providerId = userInfo.providerId,
        )
    }
}
