package com.bandage.v1.domain.member.service

import com.bandage.v1.domain.band.repository.BandMemberRepository
import com.bandage.v1.domain.jam.repository.JamParticipantRepository
import com.bandage.v1.domain.member.dto.req.MemberCreateRequest
import com.bandage.v1.domain.member.dto.req.MemberInfoUpdateRequest
import com.bandage.v1.domain.member.dto.res.MemberInfoResponse
import com.bandage.v1.domain.member.dto.res.MemberSearchItemResponse
import com.bandage.v1.domain.member.model.Member
import com.bandage.v1.domain.member.repository.MemberRepository
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import com.bandage.v1.global.infra.s3.CloudFrontUrlResolver
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberService(
    private val memberRepository: MemberRepository,
    private val bandMemberRepository: BandMemberRepository,
    private val jamParticipantRepository: JamParticipantRepository,
    private val cloudFrontUrlResolver: CloudFrontUrlResolver,
) {
    @Transactional
    fun createMember(request: MemberCreateRequest): Member {
        isMemberAlreadyExists(request)
        return memberRepository.save(
            Member.create(
                email = request.email,
                name = request.name,
                contact = request.contact,
            ),
        )
    }

    fun getMemberInfo(memberId: Long): MemberInfoResponse {
        val member = getMember(memberId)
        return MemberInfoResponse.of(member, profileImageUrl(member.profileImg))
    }

    @Transactional
    fun deleteMember(memberId: Long) {
        val member = getMember(memberId)
        member.markAsDeleted()
        memberRepository.save(member)
    }

    @Transactional
    fun updateMemberInfo(
        request: MemberInfoUpdateRequest,
        memberId: Long,
    ) {
        if (request.name == null && request.contact == null && request.profileImg == null) {
            throw BusinessException(ErrorCode.NO_CHANGE)
        }
        val member = getMember(memberId)
        var isChanged = false

        request.name
            ?.takeIf { it != member.name }
            ?.let {
                member.updateName(it)
                isChanged = true
            }
        request.contact
            ?.takeIf { it != member.contact }
            ?.let {
                member.updateContact(it)
                isChanged = true
            }
        request.profileImg
            ?.takeIf { it != member.profileImg }
            ?.let {
                member.updateProfileImg(it)
                isChanged = true
            }
        if (!isChanged) {
            throw BusinessException(ErrorCode.NO_CHANGE)
        }
    }

    fun searchMembers(
        keyword: String,
        excludeMemberId: Long?,
    ): List<MemberSearchItemResponse> {
        val q = keyword.trim()
        if (q.isEmpty()) return emptyList()
        return memberRepository
            .findTop20ByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(q, q)
            .filter { excludeMemberId == null || it.id != excludeMemberId }
            .map { MemberSearchItemResponse.of(it, profileImageUrl(it.profileImg)) }
    }

    private fun profileImageUrl(key: String?): String? = cloudFrontUrlResolver.resolveOrNull(key)

    private fun isMemberAlreadyExists(request: MemberCreateRequest) {
        if (memberRepository.existsByEmail(request.email)) {
            throw BusinessException(ErrorCode.DUPLICATE_EMAIL)
        }
    }

    private fun getMember(memberId: Long): Member =
        memberRepository.findByIdOrNull(memberId)
            ?: throw BusinessException(ErrorCode.MEMBER_NOT_FOUND)
}
