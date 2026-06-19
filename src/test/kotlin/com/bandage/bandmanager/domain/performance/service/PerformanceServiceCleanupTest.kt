package com.bandage.bandmanager.domain.performance.service

import com.bandage.bandmanager.domain.band.model.Band
import com.bandage.bandmanager.domain.band.model.BandMember
import com.bandage.bandmanager.domain.band.model.enums.BandRole
import com.bandage.bandmanager.domain.band.repository.BandMemberRepository
import com.bandage.bandmanager.domain.band.repository.BandRepository
import com.bandage.bandmanager.domain.member.repository.MemberRepository
import com.bandage.bandmanager.domain.performance.model.Performance
import com.bandage.bandmanager.domain.performance.model.PerformanceManager
import com.bandage.bandmanager.domain.performance.model.PerformanceSetlist
import com.bandage.bandmanager.domain.performance.model.enums.PerformanceRole
import com.bandage.bandmanager.domain.performance.repository.PerformanceInvitationRepository
import com.bandage.bandmanager.domain.performance.repository.PerformanceManagerRepository
import com.bandage.bandmanager.domain.performance.repository.PerformanceRepository
import com.bandage.bandmanager.domain.performance.repository.PerformanceSetlistRepository
import com.bandage.bandmanager.domain.setlist.model.SetlistBand
import com.bandage.bandmanager.domain.setlist.repository.SetlistBandRepository
import com.bandage.bandmanager.domain.setlist.repository.SetlistRepository
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import com.bandage.bandmanager.global.infra.s3.CloudFrontUrlResolver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class PerformanceServiceCleanupTest {
    private val performanceRepository = mock(PerformanceRepository::class.java)
    private val performanceSetlistRepository = mock(PerformanceSetlistRepository::class.java)
    private val performanceManagerRepository = mock(PerformanceManagerRepository::class.java)
    private val performanceInvitationRepository = mock(PerformanceInvitationRepository::class.java)
    private val setlistRepository = mock(SetlistRepository::class.java)
    private val setlistBandRepository = mock(SetlistBandRepository::class.java)
    private val bandRepository = mock(BandRepository::class.java)
    private val bandMemberRepository = mock(BandMemberRepository::class.java)
    private val memberRepository = mock(MemberRepository::class.java)
    private val cloudFrontUrlResolver = mock(CloudFrontUrlResolver::class.java)

    private val sut =
        PerformanceService(
            performanceRepository,
            performanceSetlistRepository,
            performanceManagerRepository,
            performanceInvitationRepository,
            setlistRepository,
            setlistBandRepository,
            bandRepository,
            bandMemberRepository,
            memberRepository,
            cloudFrontUrlResolver,
        )

    private val performanceId: UUID = UUID.fromString("00000000-0000-0000-0000-0000000000a1")
    private val setlistId: UUID = UUID.fromString("00000000-0000-0000-0000-0000000000b1")
    private val bandId: UUID = UUID.fromString("00000000-0000-0000-0000-0000000000c1")
    private val ownerId = 1L
    private val managerId = 2L
    private val base: LocalDateTime = LocalDateTime.of(2024, 1, 1, 0, 0)

    private fun performance(): Performance {
        val performance =
            Performance.create(title = "공연", startAt = base.plusDays(10), durationMinutes = 60, venue = null)
        setEntityId(performance, performanceId)
        `when`(performanceRepository.findById(performanceId)).thenReturn(Optional.of(performance))
        return performance
    }

    private fun owner(
        performance: Performance,
        memberId: Long,
        createdAt: LocalDateTime,
    ): PerformanceManager {
        val manager = PerformanceManager.createOwner(performance, memberId)
        manager.createdAt = createdAt
        return manager
    }

    private fun manager(
        performance: Performance,
        memberId: Long,
        createdAt: LocalDateTime,
    ): PerformanceManager {
        val manager = PerformanceManager.createManager(performance, memberId)
        manager.createdAt = createdAt
        return manager
    }

    private fun bandMember(
        band: Band,
        memberId: Long,
        createdAt: LocalDateTime,
    ): BandMember {
        val bandMember = BandMember.create(band, memberId, BandRole.MEMBER)
        bandMember.createdAt = createdAt
        return bandMember
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
    fun `OWNER 탈퇴 시 최고참 MANAGER 가 OWNER 로 승격된다`() {
        val performance = performance()
        val ownerRow = owner(performance, ownerId, base)
        val newerManager = manager(performance, 5L, base.plusDays(2))
        val olderManager = manager(performance, 6L, base.plusDays(1))
        `when`(performanceManagerRepository.findAllByMemberFetchPerformance(ownerId)).thenReturn(listOf(ownerRow))
        `when`(performanceManagerRepository.findAllByPerformanceIn(listOf(performance)))
            .thenReturn(listOf(ownerRow, newerManager, olderManager))
        `when`(performanceSetlistRepository.findAllByPerformanceIn(listOf(performance))).thenReturn(emptyList())

        sut.cleanupOnWithdrawal(ownerId)

        assertThat(olderManager.role).isEqualTo(PerformanceRole.OWNER)
        assertThat(newerManager.role).isEqualTo(PerformanceRole.MANAGER)
        verify(performanceManagerRepository).deleteAll(listOf(ownerRow))
        verify(performanceManagerRepository, never()).save(any())
    }

    @Test
    fun `MANAGER 가 없으면 셋리스트 밴드의 최고참 멤버에게 OWNER 가 부여된다`() {
        val performance = performance()
        val ownerRow = owner(performance, ownerId, base)
        `when`(performanceManagerRepository.findAllByMemberFetchPerformance(ownerId)).thenReturn(listOf(ownerRow))
        `when`(performanceManagerRepository.findAllByPerformanceIn(listOf(performance))).thenReturn(listOf(ownerRow))
        `when`(performanceSetlistRepository.findAllByPerformanceIn(listOf(performance)))
            .thenReturn(listOf(PerformanceSetlist.create(performance, setlistId)))
        `when`(setlistBandRepository.findAllBySetlistIdIn(listOf(setlistId)))
            .thenReturn(listOf(SetlistBand.create(bandId, setlistId)))

        val band = Band.create(name = "밴드", description = "설명", profileImg = null)
        setEntityId(band, bandId)
        val newer = bandMember(band, 11L, base.plusDays(1))
        val oldest = bandMember(band, 10L, base)
        `when`(bandMemberRepository.findAllByBandIdIn(listOf(bandId))).thenReturn(listOf(newer, oldest))

        var saved: PerformanceManager? = null
        `when`(performanceManagerRepository.save(any())).thenAnswer {
            saved = it.arguments[0] as PerformanceManager
            saved
        }

        sut.cleanupOnWithdrawal(ownerId)

        assertThat(saved).isNotNull()
        assertThat(saved!!.member).isEqualTo(10L)
        assertThat(saved!!.role).isEqualTo(PerformanceRole.OWNER)
        verify(performanceManagerRepository).deleteAll(listOf(ownerRow))
    }

    @Test
    fun `후임 후보가 전무하면 공연이 소프트 삭제된다`() {
        val performance = performance()
        val ownerRow = owner(performance, ownerId, base)
        `when`(performanceManagerRepository.findAllByMemberFetchPerformance(ownerId)).thenReturn(listOf(ownerRow))
        `when`(performanceManagerRepository.findAllByPerformanceIn(listOf(performance))).thenReturn(listOf(ownerRow))
        `when`(performanceSetlistRepository.findAllByPerformanceIn(listOf(performance))).thenReturn(emptyList())

        sut.cleanupOnWithdrawal(ownerId)

        assertThat(performance.deletedAt).isNotNull()
        verify(performanceManagerRepository, never()).save(any())
        verify(performanceManagerRepository).deleteAll(listOf(ownerRow))
    }

    @Test
    fun `MANAGER 만 보유한 회원은 양도 없이 권한 레코드만 제거된다`() {
        val performance = performance()
        val managerRow = manager(performance, managerId, base)
        `when`(performanceManagerRepository.findAllByMemberFetchPerformance(managerId)).thenReturn(listOf(managerRow))

        sut.cleanupOnWithdrawal(managerId)

        verify(performanceManagerRepository).deleteAll(listOf(managerRow))
        verify(performanceManagerRepository, never()).findAllByPerformanceIn(anyCollection())
        verify(performanceManagerRepository, never()).save(any())
    }

    @Test
    fun `delegateOwnership - OWNER 가 MANAGER 에게 양도하면 역할이 교체된다`() {
        val performance = performance()
        val ownerRow = owner(performance, ownerId, base)
        val target = manager(performance, managerId, base.plusDays(1))
        `when`(performanceManagerRepository.findByPerformanceAndMember(performance, ownerId)).thenReturn(ownerRow)
        `when`(performanceManagerRepository.findByPerformanceAndMember(performance, managerId)).thenReturn(target)

        sut.delegateOwnership(performanceId, managerId, ownerId)

        assertThat(ownerRow.role).isEqualTo(PerformanceRole.MANAGER)
        assertThat(target.role).isEqualTo(PerformanceRole.OWNER)
    }

    @Test
    fun `delegateOwnership - 대상이 참가자가 아니면 NOT_A_PERFORMANCE_MANAGER`() {
        val performance = performance()
        val ownerRow = owner(performance, ownerId, base)
        `when`(performanceManagerRepository.findByPerformanceAndMember(performance, ownerId)).thenReturn(ownerRow)
        `when`(performanceManagerRepository.findByPerformanceAndMember(performance, managerId)).thenReturn(null)

        val ex =
            assertThrows<BusinessException> {
                sut.delegateOwnership(performanceId, managerId, ownerId)
            }
        assertThat(ex.errorCode).isEqualTo(ErrorCode.NOT_A_PERFORMANCE_MANAGER)
    }
}
