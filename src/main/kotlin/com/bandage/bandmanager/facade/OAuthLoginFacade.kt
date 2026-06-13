package com.bandage.bandmanager.facade

import com.bandage.bandmanager.domain.auth.model.MemberAuth
import com.bandage.bandmanager.domain.auth.model.enums.ProviderType
import com.bandage.bandmanager.domain.auth.oauth.OAuthUserInfo
import com.bandage.bandmanager.domain.auth.service.MemberAuthService
import com.bandage.bandmanager.domain.member.model.Member
import com.bandage.bandmanager.domain.member.repository.MemberRepository
import com.bandage.bandmanager.facade.dto.OAuthLoginResponse
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
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
