package com.bandage.v1.domain.auth.service

import com.bandage.v1.domain.auth.dto.req.MemberAuthCreateRequest
import com.bandage.v1.domain.auth.dto.req.MemberLoginRequest
import com.bandage.v1.domain.auth.dto.res.TokenDto
import com.bandage.v1.domain.auth.model.MemberAuth
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

    fun processLogin(request: MemberLoginRequest): TokenDto {
        val memberAuth =
            memberAuthRepository.findByEmail(request.email)
                ?: throw BusinessException(ErrorCode.MEMBER_NOT_FOUND)
        if (!passwordEncoder.matches(request.password, memberAuth.password)) {
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

    fun processLogout(memberId: Long) {
        refreshTokenRepository.delete(memberId)
    }

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
