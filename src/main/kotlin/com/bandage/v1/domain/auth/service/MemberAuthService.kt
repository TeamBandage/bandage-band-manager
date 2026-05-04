package com.bandage.v1.domain.auth.service

import com.bandage.v1.domain.auth.dto.req.MemberAuthCreateRequest
import com.bandage.v1.domain.auth.dto.req.MemberLoginRequest
import com.bandage.v1.domain.auth.dto.req.MemberPasswordChangeRequest
import com.bandage.v1.domain.auth.dto.res.TokenDto
import com.bandage.v1.domain.auth.model.MemberAuth
import com.bandage.v1.domain.auth.model.enums.ProviderType
import com.bandage.v1.domain.auth.repository.MemberAuthRepository
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import com.bandage.v1.global.properties.JwtProperties
import com.bandage.v1.global.security.jwt.JwtProvider
import com.bandage.v1.global.security.jwt.RefreshTokenRepository
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberAuthService(
    private val memberAuthRepository: MemberAuthRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
    private val jwtProvider: JwtProvider,
    private val jwtProperties: JwtProperties,
) {
    @Transactional
    fun createMemberAuth(request: MemberAuthCreateRequest): MemberAuth {
        isMemberAuthAlreadyExists(request)
        return memberAuthRepository.save(
            MemberAuth.create(
                memberId = request.memberId,
                email = request.email,
                password = encodePassword(request.rawPassword),
            ),
        )
    }

    @Transactional
    fun createOAuthMemberAuth(
        memberId: Long,
        email: String,
        provider: ProviderType,
        providerId: String,
    ): MemberAuth {
        if (memberAuthRepository.existsByMemberId(memberId)) {
            throw BusinessException(ErrorCode.MEMBER_AUTH_ALREADY_EXISTS)
        }
        return memberAuthRepository.save(
            MemberAuth.createOAuth(
                memberId = memberId,
                email = email,
                provider = provider,
                providerId = providerId,
            ),
        )
    }

    fun findByEmail(email: String): MemberAuth? = memberAuthRepository.findByEmail(email)

    @Transactional
    fun issueTokens(memberAuth: MemberAuth): TokenDto {
        val accessToken = jwtProvider.createAccessToken(memberAuth.memberId, memberAuth.role)
        val refreshToken = jwtProvider.createRefreshToken(memberAuth.memberId)
        refreshTokenRepository.save(
            memberId = memberAuth.memberId,
            refreshToken = refreshToken,
            expiration = jwtProperties.refreshTokenExpr,
        )
        return TokenDto(accessToken = accessToken, refreshToken = refreshToken)
    }

    @Transactional
    fun processLogin(request: MemberLoginRequest): TokenDto {
        val memberAuth =
            memberAuthRepository.findByEmail(request.email)
                ?: throw BusinessException(ErrorCode.MEMBER_NOT_FOUND)
        val storedPassword =
            memberAuth.password
                ?: throw BusinessException(ErrorCode.OAUTH_LOCAL_LOGIN_NOT_ALLOWED)
        if (!passwordEncoder.matches(request.password, storedPassword)) {
            throw BusinessException(ErrorCode.INVALID_PASSWORD)
        }
        val memberId = memberAuth.memberId

        val accessToken = jwtProvider.createAccessToken(memberId, memberAuth.role)
        val refreshToken = jwtProvider.createRefreshToken(memberId)
        val expiration = jwtProperties.refreshTokenExpr

        refreshTokenRepository.save(
            memberId = memberAuth.memberId,
            refreshToken = refreshToken,
            expiration = expiration,
        )

        return TokenDto(
            accessToken = accessToken,
            refreshToken = refreshToken,
        )
    }

    @Transactional
    fun processLogout(memberId: Long) {
        refreshTokenRepository.delete(memberId)
    }

    @Transactional
    fun reissueToken(oldRefreshToken: String): TokenDto {
        val memberId = jwtProvider.getMemberIdFromToken(oldRefreshToken)
        validateRefreshToken(oldRefreshToken, memberId)
        val memberAuth =
            memberAuthRepository.findByMemberId(memberId)
                ?: throw BusinessException(ErrorCode.MEMBER_AUTH_NOT_FOUND)

        val newAccessToken = jwtProvider.createAccessToken(memberAuth.memberId, memberAuth.role)
        val newRefreshToken = jwtProvider.createRefreshToken(memberAuth.memberId)
        val expiration = jwtProperties.refreshTokenExpr

        refreshTokenRepository.save(
            memberId = memberAuth.memberId,
            refreshToken = newRefreshToken,
            expiration = expiration,
        )

        return TokenDto(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
        )
    }

    @Transactional
    fun deleteMemberAuth(memberId: Long) {
        val memberAuth = getMemberAuth(memberId)
        memberAuth.markAsDeleted()
        memberAuthRepository.save(memberAuth)
    }

    @Transactional
    fun changePassword(
        request: MemberPasswordChangeRequest,
        memberId: Long,
    ) {
        val memberAuth = getMemberAuth(memberId)
        val storedPassword =
            memberAuth.password
                ?: throw BusinessException(ErrorCode.OAUTH_PASSWORD_CHANGE_NOT_ALLOWED)
        if (!passwordEncoder.matches(request.originalPassword, storedPassword)) {
            throw BusinessException(ErrorCode.INVALID_PASSWORD)
        }
        if (passwordEncoder.matches(request.newPassword, storedPassword)) {
            throw BusinessException(ErrorCode.DUPLICATE_PASSWORD)
        }
        memberAuth.updatePassword(encodePassword(request.newPassword))
        refreshTokenRepository.delete(memberId) // member logout
    }

    private fun isMemberAuthAlreadyExists(request: MemberAuthCreateRequest) {
        if (memberAuthRepository.existsByMemberId(request.memberId)) {
            throw BusinessException(ErrorCode.MEMBER_AUTH_ALREADY_EXISTS)
        }
    }

    private fun getMemberAuth(memberId: Long): MemberAuth =
        memberAuthRepository.findByMemberId(memberId)
            ?: throw BusinessException(ErrorCode.MEMBER_NOT_FOUND)

    private fun encodePassword(rawPassword: String): String =
        passwordEncoder.encode(rawPassword)
            ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)

    private fun validateRefreshToken(
        refreshToken: String,
        memberId: Long,
    ) {
        if (!jwtProvider.validateToken(refreshToken)) throw BusinessException(ErrorCode.INVALID_REFRESH_TOKEN)
        val savedRefreshToken =
            refreshTokenRepository.get(memberId)
                ?: throw BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN)
        if (refreshToken != savedRefreshToken) throw BusinessException(ErrorCode.INVALID_REFRESH_TOKEN)
    }
}
