package com.bandage.bandmanager.domain.band.service

import com.bandage.bandmanager.domain.band.model.Band
import com.bandage.bandmanager.domain.band.model.BandMember
import com.bandage.bandmanager.domain.band.model.enums.ApplicationStatus
import com.bandage.bandmanager.domain.band.model.enums.BandRole
import com.bandage.bandmanager.domain.band.repository.BandApplicationRepository
import com.bandage.bandmanager.domain.band.repository.BandMemberRepository
import com.bandage.bandmanager.domain.band.repository.BandRepository
import com.bandage.bandmanager.domain.member.repository.MemberRepository
import com.bandage.bandmanager.global.infra.s3.CloudFrontUrlResolver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.UUID

class BandServiceCleanupTest {
    private val bandRepository = mock(BandRepository::class.java)
    private val applicationRepository = mock(BandApplicationRepository::class.java)
    private val bandMemberRepository = mock(BandMemberRepository::class.java)
    private val memberRepository = mock(MemberRepository::class.java)
    private val cloudFrontUrlResolver = mock(CloudFrontUrlResolver::class.java)

    private val sut =
        BandService(
            bandRepository,
            applicationRepository,
            bandMemberRepository,
            memberRepository,
            cloudFrontUrlResolver,
        )

    private val bandId: UUID = UUID.fromString("00000000-0000-0000-0000-0000000000a1")
    private val base: LocalDateTime = LocalDateTime.of(2024, 1, 1, 0, 0)

    private fun band(): Band {
        val band = Band.create(name = "밴드", description = "설명", profileImg = null)
        setEntityId(band, bandId)
        return band
    }

    private fun member(
        band: Band,
        memberId: Long,
        role: BandRole,
        createdAt: LocalDateTime,
    ): BandMember {
        val bandMember = BandMember.create(band, memberId, role)
        bandMember.createdAt = createdAt
        return bandMember
    }

    private fun stub(
        members: List<BandMember>,
        leavingId: Long,
    ) {
        `when`(bandMemberRepository.findAllBandIdsByMember(leavingId)).thenReturn(listOf(bandId))
        `when`(bandMemberRepository.findAllByBandIdIn(listOf(bandId))).thenReturn(members)
        `when`(applicationRepository.findAllByMemberAndStatusFetchBand(leavingId, ApplicationStatus.APPROVED))
            .thenReturn(emptyList())
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
    fun `리더 탈퇴 시 ADMIN 이 MEMBER 보다 우선해 리더를 승계한다`() {
        val band = band()
        val leaving = member(band, 1L, BandRole.LEADER, base)
        val oldestMember = member(band, 2L, BandRole.MEMBER, base.minusDays(5)) // 더 오래됐지만 일반 멤버
        val admin = member(band, 3L, BandRole.ADMIN, base.plusDays(1)) // 더 최근이지만 상위 티어

        stub(listOf(leaving, oldestMember, admin), 1L)

        sut.cleanupOnWithdrawal(1L)

        assertThat(admin.role).isEqualTo(BandRole.LEADER)
        assertThat(leaving.role).isEqualTo(BandRole.MEMBER)
        assertThat(leaving.deletedAt).isNotNull()
    }

    @Test
    fun `리더 탈퇴 시 ADMIN 이 없으면 가장 오래된 MEMBER 가 승계한다`() {
        val band = band()
        val leaving = member(band, 1L, BandRole.LEADER, base)
        val newer = member(band, 2L, BandRole.MEMBER, base.plusDays(2))
        val oldest = member(band, 3L, BandRole.MEMBER, base.plusDays(1))

        stub(listOf(leaving, newer, oldest), 1L)

        sut.cleanupOnWithdrawal(1L)

        assertThat(oldest.role).isEqualTo(BandRole.LEADER)
        assertThat(newer.role).isEqualTo(BandRole.MEMBER)
        assertThat(leaving.deletedAt).isNotNull()
    }

    @Test
    fun `마지막 멤버인 밴드는 소프트 삭제된다`() {
        val band = band()
        val leaving = member(band, 1L, BandRole.LEADER, base)

        stub(listOf(leaving), 1L)

        sut.cleanupOnWithdrawal(1L)

        assertThat(band.deletedAt).isNotNull()
        assertThat(leaving.deletedAt).isNotNull()
    }

    @Test
    fun `리더가 아니면 양도 없이 본인만 정리된다`() {
        val band = band()
        val leader = member(band, 1L, BandRole.LEADER, base)
        val leaving = member(band, 2L, BandRole.MEMBER, base.plusDays(1))

        stub(listOf(leader, leaving), 2L)

        sut.cleanupOnWithdrawal(2L)

        assertThat(leader.role).isEqualTo(BandRole.LEADER)
        assertThat(leaving.deletedAt).isNotNull()
        assertThat(band.deletedAt).isNull()
    }
}
