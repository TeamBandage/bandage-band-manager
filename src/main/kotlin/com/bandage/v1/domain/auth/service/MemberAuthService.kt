package com.bandage.v1.domain.auth.service

import com.bandage.v1.domain.auth.dto.req.MemberAuthCreateRequest
import com.bandage.v1.domain.auth.dto.req.MemberLoginRequest
import com.bandage.v1.domain.auth.dto.res.TokenDto
import com.bandage.v1.domain.auth.model.MemberAuth
import com.bandage.v1.domain.auth.repository.MemberAuthRepository
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import com.bandage.v1.global.security.jwt.JwtProvider
import com.bandage.v1.global.security.jwt.RefreshTokenRepository
import com.bandage.v1.global.util.SecurityUtil
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
) {
    @Transactional
    fun createMemberAuth(request: MemberAuthCreateRequest): MemberAuth {
        isMemberAuthAlreadyExists(request)
        return memberAuthRepository.save(
            MemberAuth.create(
                memberId = request.memberId,
                email = request.email,
                password = request.password,
                role = request.role,
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

        refreshTokenRepository.save(memberId, refreshToken)

        return TokenDto(
            accessToken = accessToken,
            refreshToken = refreshToken,
        )
    }

    fun processLogout() {
        refreshTokenRepository.delete(SecurityUtil.getCurrentMemberId())
    }

    fun reissueToken(oldRefreshToken: String): TokenDto {
        val memberId = jwtProvider.getMemberIdFromToken(oldRefreshToken)
        refreshTokenRepository.validate(oldRefreshToken, memberId)
        val memberAuth =
            memberAuthRepository.findByMemberId(memberId)
                ?: throw BusinessException(ErrorCode.MEMBER_AUTH_NOT_FOUND)

        val newAccessToken = jwtProvider.createAccessToken(memberAuth.memberId, memberAuth.role)
        val newRefreshToken = jwtProvider.createRefreshToken(memberAuth.memberId)

        refreshTokenRepository.save(memberAuth.memberId, newRefreshToken)

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
}
