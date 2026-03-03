package com.bandage.v1.domain.member.service

import com.bandage.v1.domain.member.dto.req.MemberLoginRequest
import com.bandage.v1.domain.member.dto.res.TokenDto
import com.bandage.v1.domain.member.repository.MemberRepository
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import com.bandage.v1.global.security.jwt.JwtProvider
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

@Service
class MemberAuthService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
    private val jwtProvider: JwtProvider,
) {
    fun processLogin(request: MemberLoginRequest): TokenDto {
        val member =
            memberRepository.findByEmail(request.email)
                ?: throw BusinessException(ErrorCode.MEMBER_NOT_FOUND)

        if (!passwordEncoder.matches(request.password, member.password)) {
            throw BusinessException(ErrorCode.INVALID_PASSWORD)
        }

        return TokenDto(
            accessToken = jwtProvider.createAccessToken(member.id!!, member.role),
            refreshToken = jwtProvider.createRefreshToken(member.id!!),
        )
    }
}
