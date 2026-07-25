package com.bandage.bandmanager.domain.selection.service

import com.bandage.bandmanager.domain.band.model.Band
import com.bandage.bandmanager.domain.band.model.BandMember
import com.bandage.bandmanager.domain.band.model.enums.BandRole
import com.bandage.bandmanager.domain.band.repository.BandMemberRepository
import com.bandage.bandmanager.domain.member.service.MemberService
import com.bandage.bandmanager.domain.selection.model.TrackSelection
import com.bandage.bandmanager.domain.selection.model.TrackSelectionBand
import com.bandage.bandmanager.domain.selection.model.TrackSelectionMember
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionBandRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionItemApplicantRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionItemChatMessageRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionItemConfirmationRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionItemRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionMemberRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.UUID

class TrackSelectionServiceCleanupTest {
    private val selectionRepository = mock(TrackSelectionRepository::class.java)
    private val selectionMemberRepository = mock(TrackSelectionMemberRepository::class.java)
    private val selectionBandRepository = mock(TrackSelectionBandRepository::class.java)
    private val itemRepository = mock(TrackSelectionItemRepository::class.java)
    private val applicantRepository = mock(TrackSelectionItemApplicantRepository::class.java)
    private val confirmationRepository = mock(TrackSelectionItemConfirmationRepository::class.java)
    private val chatMessageRepository = mock(TrackSelectionItemChatMessageRepository::class.java)
    private val bandMemberRepository = mock(BandMemberRepository::class.java)
    private val memberService = mock(MemberService::class.java)

    private val sut =
        TrackSelectionService(
            selectionRepository,
            selectionMemberRepository,
            selectionBandRepository,
            itemRepository,
            applicantRepository,
            confirmationRepository,
            chatMessageRepository,
            bandMemberRepository,
            memberService,
        )

    private val selectionId: UUID = UUID.fromString("00000000-0000-0000-0000-0000000000a1")
    private val bandId: UUID = UUID.fromString("00000000-0000-0000-0000-0000000000c1")
    private val managerId = 1L
    private val base: LocalDateTime = LocalDateTime.of(2024, 1, 1, 0, 0)

    private fun selection(): TrackSelection {
        val selection =
            TrackSelection.create(
                title = "선곡 회의",
                managerId = managerId,
            )
        setEntityId(selection, selectionId)
        `when`(selectionRepository.findAllByManagerId(managerId)).thenReturn(listOf(selection))
        return selection
    }

    private fun member(
        selection: TrackSelection,
        memberId: Long,
        createdAt: LocalDateTime,
    ): TrackSelectionMember {
        val member = TrackSelectionMember.create(selection, memberId)
        member.createdAt = createdAt
        return member
    }

    private fun band(): Band {
        val band = Band.create(name = "밴드", description = "설명", profileImg = null)
        setEntityId(band, bandId)
        return band
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
    fun `매니저 탈퇴 시 회의 참여자 최고참이 매니저를 승계한다`() {
        val selection = selection()
        `when`(selectionMemberRepository.findAllBySelectionIn(listOf(selection)))
            .thenReturn(
                listOf(
                    member(selection, managerId, base),
                    member(selection, 5L, base.plusDays(2)),
                    member(selection, 6L, base.plusDays(1)),
                ),
            )

        sut.cleanupOnWithdrawal(managerId)

        assertThat(selection.managerId).isEqualTo(6L)
        assertThat(selection.deletedAt).isNull()
        verify(selectionMemberRepository, never()).save(any())
    }

    @Test
    fun `참여자가 매니저뿐이면 밴드 멤버가 승계하고 참여자로 등록된다`() {
        val selection = selection()
        `when`(selectionMemberRepository.findAllBySelectionIn(listOf(selection)))
            .thenReturn(listOf(member(selection, managerId, base)))
        `when`(selectionBandRepository.findAllBySelectionIn(listOf(selection)))
            .thenReturn(listOf(TrackSelectionBand.create(selection = selection, bandId = bandId)))
        val band = band()
        `when`(bandMemberRepository.findAllByBandIdIn(listOf(bandId)))
            .thenReturn(listOf(bandMember(band, 10L, base), bandMember(band, 11L, base.plusDays(1))))

        var saved: TrackSelectionMember? = null
        `when`(selectionMemberRepository.save(any())).thenAnswer {
            saved = it.arguments[0] as TrackSelectionMember
            saved
        }

        sut.cleanupOnWithdrawal(managerId)

        assertThat(selection.managerId).isEqualTo(10L)
        assertThat(saved).isNotNull()
        assertThat(saved!!.memberId).isEqualTo(10L)
    }

    @Test
    fun `후보가 전무하면 선곡 회의가 소프트 삭제된다`() {
        val selection = selection()
        `when`(selectionMemberRepository.findAllBySelectionIn(listOf(selection)))
            .thenReturn(listOf(member(selection, managerId, base)))

        sut.cleanupOnWithdrawal(managerId)

        assertThat(selection.deletedAt).isNotNull()
        verify(selectionMemberRepository, never()).save(any())
    }
}
