package com.bandage.v1.domain.auth.service

import com.bandage.v1.domain.auth.dto.req.MemberLoginRequest
import com.bandage.v1.domain.auth.dto.res.TokenDto
import com.bandage.v1.domain.auth.repository.MemberAuthRepository
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import com.bandage.v1.global.security.jwt.JwtProvider
import com.bandage.v1.global.security.jwt.RefreshTokenService
import com.bandage.v1.global.util.SecurityUtil
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

@Service
class MemberAuthService(
    private val authRepository: MemberAuthRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
    private val jwtProvider: JwtProvider,
    private val refreshTokenService: RefreshTokenService,
) {
    fun processLogin(request: MemberLoginRequest): TokenDto {
        val member =
            authRepository.findByEmail(request.email)
                ?: throw BusinessException(ErrorCode.MEMBER_NOT_FOUND)

        if (!passwordEncoder.matches(request.password, member.password)) {
            throw BusinessException(ErrorCode.INVALID_PASSWORD)
        }

        val memberId: Long = member.id ?: throw BusinessException(ErrorCode.MEMBER_ID_NOT_FOUND)
        val accessToken = jwtProvider.createAccessToken(memberId, member.role)
        val refreshToken = jwtProvider.createRefreshToken(memberId)

        refreshTokenService.saveRefreshToken(memberId, refreshToken)

        return TokenDto(
            accessToken = accessToken,
            refreshToken = refreshToken,
        )
    }

    fun processLogout() {
        refreshTokenService.deleteRefreshToken(memberId = SecurityUtil.getCurrentMemberId())
    }
}
