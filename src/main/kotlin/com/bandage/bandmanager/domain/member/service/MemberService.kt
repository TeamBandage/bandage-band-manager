package com.bandage.bandmanager.domain.member.service

import com.bandage.bandmanager.domain.band.repository.BandMemberRepository
import com.bandage.bandmanager.domain.jam.repository.JamParticipantRepository
import com.bandage.bandmanager.domain.member.dto.req.MemberCreateRequest
import com.bandage.bandmanager.domain.member.dto.req.MemberInfoUpdateRequest
import com.bandage.bandmanager.domain.member.dto.res.MemberInfoResponse
import com.bandage.bandmanager.domain.member.dto.res.MemberSearchItemResponse
import com.bandage.bandmanager.domain.member.dto.res.MemberSummary
import com.bandage.bandmanager.domain.member.model.Member
import com.bandage.bandmanager.domain.member.repository.MemberRepository
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import com.bandage.bandmanager.global.infra.s3.CloudFrontUrlResolver
import com.bandage.bandmanager.global.infra.s3.ImagePresignRequest
import com.bandage.bandmanager.global.infra.s3.ImagePresignResponse
import com.bandage.bandmanager.global.infra.s3.ImagePresignSupport
import com.bandage.bandmanager.global.infra.s3.S3ObjectValidator
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
    private val imagePresignSupport: ImagePresignSupport,
    private val s3ObjectValidator: S3ObjectValidator,
) {
    @Transactional
    fun createMember(request: MemberCreateRequest): Member {
        isMemberAlreadyExists(request)
        return memberRepository.save(
            Member.create(
                email = request.email,
                name = request.name,
            ),
        )
    }

    fun getMemberInfo(memberId: Long): MemberInfoResponse {
        val member = getMember(memberId)
        return MemberInfoResponse.of(member, profileImageUrl(member.profileImg))
    }

    /** 다른 도메인 응답에 회원 정보를 임베드할 때 사용. memberId → MemberSummary 맵을 한 번의 조회로 반환(N+1 방지). */
    fun getMemberSummaries(ids: Collection<Long>): Map<Long, MemberSummary> {
        if (ids.isEmpty()) return emptyMap()
        return memberRepository
            .findAllByIdIn(ids.toSet())
            .associateBy({ it.id }, { MemberSummary.of(it, profileImageUrl(it.profileImg)) })
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
        if (request.name == null && request.profileImg == null) {
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
        request.profileImg
            ?.takeIf { it != member.profileImg }
            ?.let {
                s3ObjectValidator.requireExists(it)
                member.updateProfileImg(it)
                isChanged = true
            }
        if (!isChanged) {
            throw BusinessException(ErrorCode.NO_CHANGE)
        }
    }

    @Transactional
    fun deleteProfileImage(memberId: Long) {
        val member = getMember(memberId)
        member.updateProfileImg(null)
    }

    /** 회원 본인 프로필 이미지 업로드용 presigned URL 발급. 응답 objectKey 를 회원 정보 수정 시 profileImg 로 전달. */
    fun issueProfileImagePresignedUrl(
        request: ImagePresignRequest,
        memberId: Long,
    ): ImagePresignResponse = imagePresignSupport.issue(request, "profile/member/$memberId")

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
