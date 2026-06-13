package com.bandage.bandmanager.domain.performance.service

import com.bandage.bandmanager.domain.band.repository.BandMemberRepository
import com.bandage.bandmanager.domain.band.repository.BandRepository
import com.bandage.bandmanager.domain.member.repository.MemberRepository
import com.bandage.bandmanager.domain.performance.dto.req.PerformanceInvitationCreateRequest
import com.bandage.bandmanager.domain.performance.dto.req.PerformanceSetlistAddRequest
import com.bandage.bandmanager.domain.performance.dto.req.PerformanceUpdateRequest
import com.bandage.bandmanager.domain.performance.model.Performance
import com.bandage.bandmanager.domain.performance.model.PerformanceInvitation
import com.bandage.bandmanager.domain.performance.model.PerformanceManager
import com.bandage.bandmanager.domain.performance.model.PerformanceSetlist
import com.bandage.bandmanager.domain.performance.model.enums.PerformanceInvitationStatus
import com.bandage.bandmanager.domain.performance.repository.PerformanceInvitationRepository
import com.bandage.bandmanager.domain.performance.repository.PerformanceManagerRepository
import com.bandage.bandmanager.domain.performance.repository.PerformanceRepository
import com.bandage.bandmanager.domain.performance.repository.PerformanceSetlistRepository
import com.bandage.bandmanager.domain.setlist.model.Setlist
import com.bandage.bandmanager.domain.setlist.repository.SetlistBandRepository
import com.bandage.bandmanager.domain.setlist.repository.SetlistRepository
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import com.bandage.bandmanager.global.infra.s3.CloudFrontUrlResolver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class PerformanceServiceTest {
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
    private val ownerId = 1L
    private val managerId = 2L
    private val invitedId = 3L

    private fun registeredPerformance(): Performance {
        val performance =
            Performance.create(
                title = "정기공연",
                startAt = LocalDateTime.now().plusDays(1),
                durationMinutes = 90,
                venue = null,
            )
        setEntityId(performance, performanceId)
        `when`(performanceRepository.findById(performanceId)).thenReturn(Optional.of(performance))
        return performance
    }

    private fun stubParticipant(
        performance: Performance,
        memberId: Long,
        owner: Boolean,
    ) {
        val manager =
            if (owner) {
                PerformanceManager.createOwner(performance, memberId)
            } else {
                PerformanceManager.createManager(performance, memberId)
            }
        `when`(performanceManagerRepository.findByPerformanceAndMember(performance, memberId)).thenReturn(manager)
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
    fun `addSetlists - 본인이 접근 불가한 셋리스트는 SETLIST_FORBIDDEN`() {
        val performance = registeredPerformance()
        stubParticipant(performance, managerId, owner = false)
        `when`(performanceSetlistRepository.existsByPerformanceAndSetlistId(performance, setlistId)).thenReturn(false)
        `when`(setlistRepository.findById(setlistId)).thenReturn(Optional.of(mock(Setlist::class.java)))
        `when`(setlistRepository.isAccessibleMember(setlistId, managerId)).thenReturn(false)

        val ex =
            assertThrows<BusinessException> {
                sut.addSetlists(performanceId, PerformanceSetlistAddRequest(setlistIds = listOf(setlistId)), managerId)
            }
        assertThat(ex.errorCode).isEqualTo(ErrorCode.SETLIST_FORBIDDEN)
        verify(performanceSetlistRepository, never()).save(any())
    }

    @Test
    fun `removeSetlist - MANAGER가 접근 불가한 셋리스트 제거 시 거부된다`() {
        val performance = registeredPerformance()
        stubParticipant(performance, managerId, owner = false)
        `when`(performanceSetlistRepository.findByPerformanceAndSetlistId(performance, setlistId))
            .thenReturn(PerformanceSetlist.create(performance, setlistId))
        `when`(setlistRepository.isAccessibleMember(setlistId, managerId)).thenReturn(false)

        val ex = assertThrows<BusinessException> { sut.removeSetlist(performanceId, setlistId, managerId) }
        assertThat(ex.errorCode).isEqualTo(ErrorCode.SETLIST_FORBIDDEN)
        verify(performanceSetlistRepository, never()).delete(any())
    }

    @Test
    fun `removeSetlist - OWNER는 접근 불가한 셋리스트도 제거할 수 있다`() {
        val performance = registeredPerformance()
        stubParticipant(performance, ownerId, owner = true)
        val ps = PerformanceSetlist.create(performance, setlistId)
        `when`(performanceSetlistRepository.findByPerformanceAndSetlistId(performance, setlistId)).thenReturn(ps)

        sut.removeSetlist(performanceId, setlistId, ownerId)

        verify(performanceSetlistRepository).delete(ps)
        verify(setlistRepository, never()).isAccessibleMember(setlistId, ownerId)
    }

    @Test
    fun `deletePerformance - MANAGER는 OWNER 권한이 아니어서 거부된다`() {
        val performance = registeredPerformance()
        stubParticipant(performance, managerId, owner = false)

        val ex = assertThrows<BusinessException> { sut.deletePerformance(performanceId, managerId) }
        assertThat(ex.errorCode).isEqualTo(ErrorCode.NOT_A_PERFORMANCE_OWNER)
    }

    @Test
    fun `deletePerformance - OWNER는 삭제할 수 있다`() {
        val performance = registeredPerformance()
        stubParticipant(performance, ownerId, owner = true)

        sut.deletePerformance(performanceId, ownerId)

        assertThat(performance.deletedAt).isNotNull()
    }

    @Test
    fun `updatePerformance - MANAGER도 수정할 수 있다`() {
        val performance = registeredPerformance()
        stubParticipant(performance, managerId, owner = false)

        sut.updatePerformance(performanceId, PerformanceUpdateRequest(title = "수정된 제목"), managerId)

        assertThat(performance.title).isEqualTo("수정된 제목")
    }

    @Test
    fun `sendInvitation - OWNER가 아니면 거부된다`() {
        val performance = registeredPerformance()
        stubParticipant(performance, managerId, owner = false)

        val ex =
            assertThrows<BusinessException> {
                sut.sendInvitation(performanceId, PerformanceInvitationCreateRequest(memberId = invitedId), managerId)
            }
        assertThat(ex.errorCode).isEqualTo(ErrorCode.NOT_A_PERFORMANCE_OWNER)
    }

    @Test
    fun `sendInvitation - 존재하지 않는 멤버면 MEMBER_NOT_FOUND`() {
        val performance = registeredPerformance()
        stubParticipant(performance, ownerId, owner = true)
        `when`(memberRepository.existsById(invitedId)).thenReturn(false)

        val ex =
            assertThrows<BusinessException> {
                sut.sendInvitation(performanceId, PerformanceInvitationCreateRequest(memberId = invitedId), ownerId)
            }
        assertThat(ex.errorCode).isEqualTo(ErrorCode.MEMBER_NOT_FOUND)
    }

    @Test
    fun `sendInvitation - 이미 공연 참가자면 ALREADY_PERFORMANCE_MANAGER`() {
        val performance = registeredPerformance()
        stubParticipant(performance, ownerId, owner = true)
        `when`(memberRepository.existsById(invitedId)).thenReturn(true)
        `when`(performanceManagerRepository.existsByPerformanceAndMember(performance, invitedId)).thenReturn(true)

        val ex =
            assertThrows<BusinessException> {
                sut.sendInvitation(performanceId, PerformanceInvitationCreateRequest(memberId = invitedId), ownerId)
            }
        assertThat(ex.errorCode).isEqualTo(ErrorCode.ALREADY_PERFORMANCE_MANAGER)
    }

    @Test
    fun `sendInvitation - 대기 중 초대가 이미 있으면 거부된다`() {
        val performance = registeredPerformance()
        stubParticipant(performance, ownerId, owner = true)
        `when`(memberRepository.existsById(invitedId)).thenReturn(true)
        `when`(performanceManagerRepository.existsByPerformanceAndMember(performance, invitedId)).thenReturn(false)
        `when`(
            performanceInvitationRepository.existsByPerformanceAndInvitedMemberAndStatus(
                performance,
                invitedId,
                PerformanceInvitationStatus.PENDING,
            ),
        ).thenReturn(true)

        val ex =
            assertThrows<BusinessException> {
                sut.sendInvitation(performanceId, PerformanceInvitationCreateRequest(memberId = invitedId), ownerId)
            }
        assertThat(ex.errorCode).isEqualTo(ErrorCode.PERFORMANCE_INVITATION_ALREADY_EXISTS)
    }

    @Test
    fun `sendInvitation - 정상 발송 시 PENDING 초대를 반환한다`() {
        val performance = registeredPerformance()
        stubParticipant(performance, ownerId, owner = true)
        `when`(memberRepository.existsById(invitedId)).thenReturn(true)
        `when`(performanceManagerRepository.existsByPerformanceAndMember(performance, invitedId)).thenReturn(false)
        `when`(
            performanceInvitationRepository.existsByPerformanceAndInvitedMemberAndStatus(
                performance,
                invitedId,
                PerformanceInvitationStatus.PENDING,
            ),
        ).thenReturn(false)
        `when`(performanceInvitationRepository.save(any())).thenAnswer {
            val saved = it.arguments[0] as PerformanceInvitation
            setEntityId(saved, UUID.fromString("00000000-0000-0000-0000-0000000000d1"))
            saved
        }
        `when`(memberRepository.findAllById(setOf(invitedId))).thenReturn(emptyList())

        val response = sut.sendInvitation(performanceId, PerformanceInvitationCreateRequest(memberId = invitedId), ownerId)

        assertThat(response.invitedMemberId).isEqualTo(invitedId)
        assertThat(response.status).isEqualTo(PerformanceInvitationStatus.PENDING)
        assertThat(response.performanceId).isEqualTo(performanceId)
    }

    @Test
    fun `respondInvitation - ACCEPTED 시 MANAGER로 추가되고 상태가 전이된다`() {
        val performance = registeredPerformance()
        val invitationId = UUID.fromString("00000000-0000-0000-0000-0000000000c1")
        val invitation = PerformanceInvitation.create(performance, invitedMember = invitedId, invitedBy = ownerId)
        `when`(performanceInvitationRepository.findByIdAndStatus(invitationId, PerformanceInvitationStatus.PENDING))
            .thenReturn(invitation)
        `when`(performanceManagerRepository.existsByPerformanceAndMember(performance, invitedId)).thenReturn(false)

        sut.respondInvitation(performanceId, invitationId, invitedId, PerformanceInvitationStatus.ACCEPTED)

        assertThat(invitation.status).isEqualTo(PerformanceInvitationStatus.ACCEPTED)
        assertThat(invitation.processedBy).isEqualTo(invitedId)
        verify(performanceManagerRepository).save(any())
    }

    @Test
    fun `respondInvitation - 수신자가 아니면 거부된다`() {
        val performance = registeredPerformance()
        val invitationId = UUID.fromString("00000000-0000-0000-0000-0000000000c2")
        val invitation = PerformanceInvitation.create(performance, invitedMember = invitedId, invitedBy = ownerId)
        `when`(performanceInvitationRepository.findByIdAndStatus(invitationId, PerformanceInvitationStatus.PENDING))
            .thenReturn(invitation)

        val ex =
            assertThrows<BusinessException> {
                sut.respondInvitation(performanceId, invitationId, 999L, PerformanceInvitationStatus.ACCEPTED)
            }
        assertThat(ex.errorCode).isEqualTo(ErrorCode.PERFORMANCE_INVITATION_FORBIDDEN)
        verify(performanceManagerRepository, never()).save(any())
    }
}
