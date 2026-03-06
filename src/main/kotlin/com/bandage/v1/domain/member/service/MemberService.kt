package com.bandage.v1.domain.member.service

import com.bandage.v1.domain.member.dto.req.MemberCreateRequest
import com.bandage.v1.domain.member.model.Member
import com.bandage.v1.domain.member.repository.MemberRepository
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
) {
    @Transactional
    fun createMember(request: MemberCreateRequest): Member {
        isMemberAlreadyExists(request)
        return memberRepository.save(
            Member.create(
                email = request.email,
                password = encodePassword(request.password),
                name = request.name,
                contact = request.contact,
            ),
        )
    }

    @Transactional
    fun deleteMember(memberId: Long) {
        val member = getMember(memberId)
        member.markAsDeleted()
        memberRepository.save(member)
    }

    private fun isMemberAlreadyExists(request: MemberCreateRequest) {
        if (memberRepository.existsByEmail(request.email)) {
            throw BusinessException(ErrorCode.DUPLICATE_EMAIL)
        }
    }

    private fun encodePassword(rawPassword: String): String =
        passwordEncoder.encode(rawPassword)
            ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)

    private fun getMember(memberId: Long): Member =
        memberRepository.findByIdOrNull(memberId)
            ?: throw BusinessException(ErrorCode.MEMBER_NOT_FOUND)
}
