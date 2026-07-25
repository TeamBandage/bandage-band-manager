package com.bandage.bandmanager.domain.selection.service

import com.bandage.bandmanager.domain.band.repository.BandMemberRepository
import com.bandage.bandmanager.domain.member.service.MemberService
import com.bandage.bandmanager.domain.selection.model.TrackSelection
import com.bandage.bandmanager.domain.selection.model.TrackSelectionItem
import com.bandage.bandmanager.domain.selection.model.TrackSelectionItemApplicant
import com.bandage.bandmanager.domain.selection.model.TrackSelectionItemConfirmation
import com.bandage.bandmanager.domain.selection.model.TrackSelectionMember
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionBandRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionItemApplicantRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionItemChatMessageRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionItemConfirmationRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionItemRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionMemberRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionRepository
import com.bandage.bandmanager.global.common.domain.TrackInfo
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.Optional
import java.util.UUID

/** 선곡 회의 떠나기(BD-218) 검증. 정책: docs/TRACK-SELECTION-LEAVE.md */
class TrackSelectionServiceLeaveTest {
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

    private val selectionId: UUID = UUID.randomUUID()
    private val managerId = 1L
    private val leaverId = 2L

    @Test
    fun `참여자가 떠나면 참여 연결이 삭제된다`() {
        val selection = selection(managerId = managerId)
        val membership = membership(selection, leaverId)
        `when`(itemRepository.findAllBySelection(selection)).thenReturn(emptyList())

        sut.leaveSelection(selectionId, leaverId)

        verify(selectionMemberRepository).delete(membership)
        // 매니저가 아니므로 권한은 그대로
        assertThat(selection.managerId).isEqualTo(managerId)
    }

    @Test
    fun `떠나는 멤버의 지원 확정이 삭제되고 해당 아이템의 선곡 확정이 해제된다`() {
        val selection = selection(managerId = managerId)
        membership(selection, leaverId)
        val affected = item(selection, proposerId = managerId, selected = true)
        val untouched = item(selection, proposerId = managerId, selected = true)
        val items = listOf(affected, untouched)
        `when`(itemRepository.findAllBySelection(selection)).thenReturn(items)
        `when`(applicantRepository.findAllByItemIn(items))
            .thenReturn(listOf(TrackSelectionItemApplicant.create(affected, "V", leaverId)))
        `when`(confirmationRepository.findAllByItemIn(items)).thenReturn(emptyList())

        sut.leaveSelection(selectionId, leaverId)

        verify(applicantRepository).deleteAllByItemInAndMemberId(items, leaverId)
        verify(confirmationRepository).deleteAllByItemInAndMemberId(items, leaverId)
        assertThat(affected.isSelected).isFalse()
        // 이 멤버와 무관한 아이템의 확정은 유지된다
        assertThat(untouched.isSelected).isTrue()
    }

    @Test
    fun `확정만 있던 아이템도 선곡 확정이 해제된다`() {
        val selection = selection(managerId = managerId)
        membership(selection, leaverId)
        val item = item(selection, proposerId = managerId, selected = true)
        val items = listOf(item)
        `when`(itemRepository.findAllBySelection(selection)).thenReturn(items)
        `when`(applicantRepository.findAllByItemIn(items)).thenReturn(emptyList())
        `when`(confirmationRepository.findAllByItemIn(items))
            .thenReturn(listOf(TrackSelectionItemConfirmation.create(item, "V", leaverId, managerId)))

        sut.leaveSelection(selectionId, leaverId)

        assertThat(item.isSelected).isFalse()
    }

    @Test
    fun `떠나는 멤버가 제안한 아이템의 제안자는 null 로 해제된다`() {
        val selection = selection(managerId = managerId)
        membership(selection, leaverId)
        val mine = item(selection, proposerId = leaverId)
        val others = item(selection, proposerId = managerId)
        val items = listOf(mine, others)
        `when`(itemRepository.findAllBySelection(selection)).thenReturn(items)
        `when`(applicantRepository.findAllByItemIn(items)).thenReturn(emptyList())
        `when`(confirmationRepository.findAllByItemIn(items)).thenReturn(emptyList())

        sut.leaveSelection(selectionId, leaverId)

        assertThat(mine.proposerId).isNull()
        assertThat(others.proposerId).isEqualTo(managerId)
    }

    @Test
    fun `매니저가 떠나면 다른 참여자에게 권한이 양도된다`() {
        val selection = selection(managerId = managerId)
        val membership = membership(selection, managerId)
        val successor = TrackSelectionMember.create(selection, leaverId)
        `when`(selectionMemberRepository.findAllBySelection(selection)).thenReturn(listOf(membership, successor))
        `when`(itemRepository.findAllBySelection(selection)).thenReturn(emptyList())

        sut.leaveSelection(selectionId, managerId)

        assertThat(selection.managerId).isEqualTo(leaverId)
        verify(selectionMemberRepository).delete(membership)
    }

    @Test
    fun `마지막 참여자인 매니저가 떠나면 회의가 소프트 삭제된다`() {
        val selection = selection(managerId = managerId)
        val membership = membership(selection, managerId)
        `when`(selectionMemberRepository.findAllBySelection(selection)).thenReturn(listOf(membership))
        `when`(selectionBandRepository.findAllBySelection(selection)).thenReturn(emptyList())
        `when`(itemRepository.findAllBySelection(selection)).thenReturn(emptyList())

        sut.leaveSelection(selectionId, managerId)

        assertThat(selection.deletedAt).isNotNull()
        // 회의를 해소했으므로 참여자 연결 삭제는 수행하지 않는다
        verify(selectionMemberRepository, never()).delete(membership)
    }

    @Test
    fun `참여자가 아니면 떠날 수 없다`() {
        val selection = selection(managerId = managerId)
        `when`(selectionMemberRepository.findBySelectionAndMemberId(selection, 99L)).thenReturn(null)

        assertThatThrownBy { sut.leaveSelection(selectionId, 99L) }
            .isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SETLIST_MEETING_FORBIDDEN)
    }

    // ---------- fixtures ----------

    private fun selection(managerId: Long): TrackSelection {
        val selection = TrackSelection.create(title = "회의", managerId = managerId)
        setId(selection, selectionId)
        `when`(selectionRepository.findById(selectionId)).thenReturn(Optional.of(selection))
        return selection
    }

    private fun membership(
        selection: TrackSelection,
        memberId: Long,
    ): TrackSelectionMember {
        val member = TrackSelectionMember.create(selection, memberId)
        `when`(selectionMemberRepository.findBySelectionAndMemberId(selection, memberId)).thenReturn(member)
        return member
    }

    private fun item(
        selection: TrackSelection,
        proposerId: Long,
        selected: Boolean = false,
    ): TrackSelectionItem {
        val item =
            TrackSelectionItem.create(
                selection = selection,
                trackInfo = TrackInfo(title = "곡", artist = "아티스트"),
                proposerId = proposerId,
                note = null,
                sessions = emptyList(),
            )
        setId(item, UUID.randomUUID())
        if (selected) item.select()
        return item
    }

    private fun setId(
        target: Any,
        id: UUID,
    ) {
        val field = target.javaClass.getDeclaredField("id")
        field.isAccessible = true
        field.set(target, id)
    }
}
