package com.bandage.bandmanager.domain.member.service

import com.bandage.bandmanager.domain.band.repository.BandMemberRepository
import com.bandage.bandmanager.domain.jam.repository.JamParticipantRepository
import com.bandage.bandmanager.domain.member.model.Member
import com.bandage.bandmanager.domain.member.repository.MemberRepository
import com.bandage.bandmanager.global.infra.s3.CloudFrontUrlResolver
import com.bandage.bandmanager.global.infra.s3.ImagePresignSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class MemberServiceSummaryTest {
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

    private fun member(
        id: Long,
        name: String,
        profileImg: String?,
    ): Member {
        // id는 @GeneratedValue라 직접 설정 불가 → mock으로 id만 stub
        val m = mock(Member::class.java)
        `when`(m.id).thenReturn(id)
        `when`(m.name).thenReturn(name)
        `when`(m.profileImg).thenReturn(profileImg)
        return m
    }

    @Test
    fun `getMemberSummaries - memberId 맵으로 변환하고 프로필 URL을 resolve한다`() {
        val m1 = member(1L, "홍길동", "profile/member/1/a.jpg")
        val m2 = member(2L, "김철수", null)
        `when`(memberRepository.findAllByIdIn(setOf(1L, 2L))).thenReturn(listOf(m1, m2))
        `when`(cloudFrontUrlResolver.resolveOrNull("profile/member/1/a.jpg")).thenReturn("https://cdn/a.jpg")
        `when`(cloudFrontUrlResolver.resolveOrNull(null)).thenReturn(null)

        val result = sut.getMemberSummaries(listOf(1L, 2L))

        assertThat(result).hasSize(2)
        assertThat(result[1L]?.name).isEqualTo("홍길동")
        assertThat(result[1L]?.profileImg).isEqualTo("https://cdn/a.jpg")
        assertThat(result[2L]?.name).isEqualTo("김철수")
        assertThat(result[2L]?.profileImg).isNull()
    }

    @Test
    fun `getMemberSummaries - 빈 입력이면 조회 없이 빈 맵을 반환한다`() {
        val result = sut.getMemberSummaries(emptyList())

        assertThat(result).isEmpty()
        verifyNoInteractions(memberRepository)
    }

    @Test
    fun `getMemberSummaries - 중복 id는 한 번만 조회한다`() {
        val m1 = member(1L, "홍길동", null)
        `when`(memberRepository.findAllByIdIn(setOf(1L))).thenReturn(listOf(m1))
        `when`(cloudFrontUrlResolver.resolveOrNull(null)).thenReturn(null)

        val result = sut.getMemberSummaries(listOf(1L, 1L, 1L))

        assertThat(result).hasSize(1)
        verify(memberRepository).findAllByIdIn(setOf(1L))
    }
}
