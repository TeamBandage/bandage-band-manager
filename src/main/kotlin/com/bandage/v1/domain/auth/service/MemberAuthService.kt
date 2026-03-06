package com.bandage.v1.domain.auth.service

import com.bandage.v1.domain.auth.dto.req.MemberLoginRequest
import com.bandage.v1.domain.auth.dto.res.TokenDto
import com.bandage.v1.domain.auth.repository.MemberAuthRepository
import com.bandage.v1.global.async.event.MemberLoginEvent
import com.bandage.v1.global.async.event.MemberLogoutEvent
import com.bandage.v1.global.async.publisher.EventPublisher
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import com.bandage.v1.global.security.jwt.JwtProvider
import com.bandage.v1.global.security.jwt.RefreshTokenService
import com.bandage.v1.global.util.SecurityUtil
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

@Service
class MemberAuthService(
    private val memberAuthRepository: MemberAuthRepository,
    private val refreshTokenService: RefreshTokenService,
    private val passwordEncoder: BCryptPasswordEncoder,
    private val jwtProvider: JwtProvider,
    private val eventPublisher: EventPublisher,
) {
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

        eventPublisher.publish(MemberLoginEvent.of(memberId, refreshToken))

        return TokenDto(
            accessToken = accessToken,
            refreshToken = refreshToken,
        )
    }

    fun processLogout() {
        eventPublisher.publish(MemberLogoutEvent.of(SecurityUtil.getCurrentMemberId()))
    }

    fun reissueToken(oldRefreshToken: String): TokenDto {
        val memberId = jwtProvider.getMemberIdFromToken(oldRefreshToken)
        refreshTokenService.validateRefreshToken(oldRefreshToken, memberId)
        val memberAuth = memberAuthRepository.findByMemberId(memberId) ?: throw BusinessException(ErrorCode.MEMBER_AUTH_NOT_FOUND)
        val newAccessToken = jwtProvider.createAccessToken(memberAuth.memberId, memberAuth.role)
        val newRefreshToken = jwtProvider.createRefreshToken(memberAuth.memberId)
        refreshTokenService.saveRefreshToken(memberAuth.memberId, newRefreshToken)
        return TokenDto(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
        )
    }
}
