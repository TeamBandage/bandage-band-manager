package com.bandage.bandmanager.domain.band.service

import com.bandage.bandmanager.domain.band.model.Band
import com.bandage.bandmanager.domain.band.model.enums.BandRole
import com.bandage.bandmanager.domain.band.repository.BandApplicationRepository
import com.bandage.bandmanager.domain.band.repository.BandMemberRepository
import com.bandage.bandmanager.domain.band.repository.BandRepository
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
import java.util.UUID

class BandServiceProfileImageTest {
    private val bandRepository = mock(BandRepository::class.java)
    private val applicationRepository = mock(BandApplicationRepository::class.java)
    private val bandMemberRepository = mock(BandMemberRepository::class.java)
    private val memberRepository = mock(MemberRepository::class.java)
    private val cloudFrontUrlResolver = mock(CloudFrontUrlResolver::class.java)
    private val imagePresignSupport = mock(ImagePresignSupport::class.java)

    private val sut =
        BandService(
            bandRepository,
            applicationRepository,
            bandMemberRepository,
            memberRepository,
            cloudFrontUrlResolver,
            imagePresignSupport,
        )

    private val bandId: UUID = UUID.fromString("00000000-0000-0000-0000-0000000000a1")
    private val leaderId = 1L
    private val nonLeaderId = 2L

    private fun bandWithProfile(profileImg: String?): Band {
        val band =
            Band.create(
                name = "테스트밴드",
                description = "설명",
                profileImg = profileImg,
            )
        setEntityId(band, bandId)
        `when`(bandRepository.findById(bandId)).thenReturn(Optional.of(band))
        return band
    }

    private fun stubLeader(
        band: Band,
        memberId: Long,
        isLeader: Boolean,
    ) {
        `when`(bandMemberRepository.existsByBandAndMemberAndRole(band, memberId, BandRole.LEADER)).thenReturn(isLeader)
    }

    private fun setEntityId(
        target: Any,
        id: UUID,
    ) {
        val field = target.javaClass.getDeclaredField("id")
        field.isAccessible = true
        field.set(target, id)
    }

    @Test
    fun `deleteProfileImage - 리더가 호출하면 프로필 이미지가 null로 변경된다`() {
        val band = bandWithProfile("profile/band/$bandId/abc.jpg")
        stubLeader(band, leaderId, isLeader = true)

        sut.deleteProfileImage(bandId, leaderId)

        assertThat(band.profileImg).isNull()
    }

    @Test
    fun `deleteProfileImage - 이미 null이어도 멱등하게 성공한다`() {
        val band = bandWithProfile(null)
        stubLeader(band, leaderId, isLeader = true)

        sut.deleteProfileImage(bandId, leaderId)

        assertThat(band.profileImg).isNull()
    }

    @Test
    fun `deleteProfileImage - 리더가 아니면 NOT_A_LEADER`() {
        val band = bandWithProfile("profile/band/$bandId/abc.jpg")
        stubLeader(band, nonLeaderId, isLeader = false)

        val ex = assertThrows<BusinessException> { sut.deleteProfileImage(bandId, nonLeaderId) }
        assertThat(ex.errorCode).isEqualTo(ErrorCode.NOT_A_LEADER)
        assertThat(band.profileImg).isEqualTo("profile/band/$bandId/abc.jpg")
    }

    @Test
    fun `deleteProfileImage - 존재하지 않는 밴드면 BAND_NOT_FOUND`() {
        `when`(bandRepository.findById(bandId)).thenReturn(Optional.empty())

        val ex = assertThrows<BusinessException> { sut.deleteProfileImage(bandId, leaderId) }
        assertThat(ex.errorCode).isEqualTo(ErrorCode.BAND_NOT_FOUND)
    }
}
