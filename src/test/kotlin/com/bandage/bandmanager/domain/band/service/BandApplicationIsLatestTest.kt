package com.bandage.bandmanager.domain.band.service

import com.bandage.bandmanager.domain.band.model.Band
import com.bandage.bandmanager.domain.band.model.BandApplication
import com.bandage.bandmanager.domain.band.model.enums.ApplicationStatus
import com.bandage.bandmanager.domain.band.repository.BandApplicationRepository
import com.bandage.bandmanager.domain.band.repository.BandMemberRepository
import com.bandage.bandmanager.domain.band.repository.BandRepository
import com.bandage.bandmanager.domain.member.repository.MemberRepository
import com.bandage.bandmanager.global.infra.s3.CloudFrontUrlResolver
import com.bandage.bandmanager.global.infra.s3.ImagePresignSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.Optional
import java.util.UUID

class BandApplicationIsLatestTest {
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

    private fun band(): Band {
        val band = Band.create(name = "밴드", description = "설명", profileImg = null)
        setEntityId(band, bandId)
        return band
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
    fun `거절된 이전 신청은 재지원 시 isLatest 가 false 로 전환된다`() {
        val band = band()
        val rejected = BandApplication.create(band, 1L)
        rejected.updateStatus(ApplicationStatus.REJECTED)

        `when`(bandRepository.findById(bandId)).thenReturn(Optional.of(band))
        `when`(bandMemberRepository.existsBandMemberByBandAndMember(band, 1L)).thenReturn(false)
        `when`(applicationRepository.existsByBandAndMemberAndStatus(band, 1L, ApplicationStatus.PENDING)).thenReturn(false)
        `when`(applicationRepository.findByBandAndMemberAndIsLatestTrue(band, 1L)).thenReturn(rejected)

        sut.createBandApplication(bandId, 1L)

        assertThat(rejected.isLatest).isFalse()
    }

    @Test
    fun `나의 신청 단건 조회는 isLatest 인 신청만 반환한다`() {
        val band = band()
        val latest = BandApplication.create(band, 1L)
        setEntityId(latest, UUID.randomUUID())

        `when`(bandRepository.findById(bandId)).thenReturn(Optional.of(band))
        `when`(applicationRepository.findByBandAndMemberAndIsLatestTrue(band, 1L)).thenReturn(latest)

        val result = sut.getMyApplicationForBand(bandId, 1L)

        assertThat(result.bandApplicationId).isEqualTo(latest.id)
    }
}
