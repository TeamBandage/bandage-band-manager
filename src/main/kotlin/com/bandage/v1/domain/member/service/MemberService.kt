package com.bandage.v1.domain.member.service

import com.bandage.v1.domain.member.dto.req.MemberJoinRequest
import com.bandage.v1.domain.member.dto.res.MemberResponse
import com.bandage.v1.domain.member.model.Member
import com.bandage.v1.domain.member.repository.MemberRepository
import com.bandage.v1.global.async.event.MemberJoinEvent
import com.bandage.v1.global.async.event.MemberWithdrawnEvent
import com.bandage.v1.global.async.publisher.EventPublisher
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import com.bandage.v1.global.util.SecurityUtil
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
    private val eventPublisher: EventPublisher,
) {
    @Transactional
    fun createMember(request: MemberJoinRequest): MemberResponse {
        validateMemberJoin(request)
        val member =
            memberRepository.save(
                Member.create(
                    email = request.email,
                    password = encodePassword(request.password),
                    name = request.name,
                    contact = request.contact,
                ),
            )
        eventPublisher.publish(MemberJoinEvent.of(member))
        return MemberResponse.of(member)
    }

    @Transactional
    fun deleteMember() {
        val member = getMember()
        member.markAsDeleted()
        memberRepository.save(member)
        eventPublisher.publish(MemberWithdrawnEvent.of(member.id!!))
    }

    private fun validateMemberJoin(request: MemberJoinRequest) {
        if (memberRepository.existsMemberByEmail(request.email)) {
            throw BusinessException(ErrorCode.DUPLICATE_EMAIL)
        }
    }

    private fun encodePassword(rawPassword: String): String =
        passwordEncoder.encode(rawPassword)
            ?: throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)

    private fun getMember(): Member =
        memberRepository.findByIdOrNull(SecurityUtil.getCurrentMemberId())
            ?: throw BusinessException(ErrorCode.MEMBER_NOT_FOUND)
}
