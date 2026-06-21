package com.bandage.bandmanager.domain.member.service

import com.bandage.bandmanager.domain.band.repository.BandMemberRepository
import com.bandage.bandmanager.domain.jam.repository.JamParticipantRepository
import com.bandage.bandmanager.domain.member.model.Member
import com.bandage.bandmanager.domain.member.repository.MemberRepository
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import com.bandage.bandmanager.global.infra.s3.CloudFrontUrlResolver
import com.bandage.bandmanager.global.infra.s3.ImagePresignSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.Optional

class MemberServiceProfileImageTest {
    private val memberRepository = mock(MemberRepository::class.java)
    private val bandMemberRepository = mock(BandMemberRepository::class.java)
    private val jamParticipantRepository = mock(JamParticipantRepository::class.java)
    private val cloudFrontUrlResolver = mock(CloudFrontUrlResolver::class.java)
    private val imagePresignSupport = mock(ImagePresignSupport::class.java)

    private val sut =
        MemberService(
            memberRepository,
            bandMemberRepository,
            jamParticipantRepository,
            cloudFrontUrlResolver,
            imagePresignSupport,
        )

    private val memberId = 1L

    private fun memberWithProfile(profileImg: String?): Member {
        val member =
            Member.create(
                email = "test@bandage.com",
                name = "테스터",
                profileImg = profileImg,
            )
        `when`(memberRepository.findById(memberId)).thenReturn(Optional.of(member))
        return member
    }

    @Test
    fun `deleteProfileImage - 프로필 이미지가 있으면 null로 변경된다`() {
        val member = memberWithProfile("profile/member/1/abc.jpg")

        sut.deleteProfileImage(memberId)

        assertThat(member.profileImg).isNull()
    }

    @Test
    fun `deleteProfileImage - 이미 null이어도 멱등하게 성공한다`() {
        val member = memberWithProfile(null)

        sut.deleteProfileImage(memberId)

        assertThat(member.profileImg).isNull()
    }

    @Test
    fun `deleteProfileImage - 존재하지 않는 회원이면 MEMBER_NOT_FOUND`() {
        `when`(memberRepository.findById(memberId)).thenReturn(Optional.empty())

        val ex = assertThrows<BusinessException> { sut.deleteProfileImage(memberId) }
        assertThat(ex.errorCode).isEqualTo(ErrorCode.MEMBER_NOT_FOUND)
    }
}
