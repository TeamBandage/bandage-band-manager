package com.bandage.bandmanager.domain.selection.service

import com.bandage.bandmanager.domain.band.repository.BandMemberRepository
import com.bandage.bandmanager.domain.member.service.MemberService
import com.bandage.bandmanager.domain.selection.dto.req.TrackSelectionManagerTransferRequest
import com.bandage.bandmanager.domain.selection.dto.req.TrackSelectionUpdateRequest
import com.bandage.bandmanager.domain.selection.model.TrackSelection
import com.bandage.bandmanager.domain.selection.model.TrackSelectionMember
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionBandRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionItemApplicantRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionItemChatMessageRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionItemConfirmationRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionItemRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionMemberRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionRepository
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.Optional
import java.util.UUID

/** 매니저 권한 양도 API 분리(BD-225) 검증. */
class TrackSelectionServiceManagerTransferTest {
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

    private val managerId = 1L
    private val newManagerId = 2L
    private val selectionId: UUID = UUID.randomUUID()

    @Test
    fun `참여자에게 매니저 권한을 양도한다`() {
        val selection = selection()
        `when`(selectionMemberRepository.findAllBySelection(selection))
            .thenReturn(listOf(TrackSelectionMember.create(selection, newManagerId)))

        sut.transferManager(selectionId, managerId, TrackSelectionManagerTransferRequest(newManagerId))

        assertThat(selection.managerId).isEqualTo(newManagerId)
    }

    @Test
    fun `참여자가 아닌 멤버로는 양도할 수 없다`() {
        val selection = selection()
        `when`(selectionMemberRepository.findAllBySelection(selection)).thenReturn(emptyList())

        assertThatThrownBy { sut.transferManager(selectionId, managerId, TrackSelectionManagerTransferRequest(newManagerId)) }
            .isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SETLIST_MANAGER_NOT_PARTICIPANT)
    }

    @Test
    fun `본인에게 양도하면 NO_CHANGE`() {
        selection()

        assertThatThrownBy { sut.transferManager(selectionId, managerId, TrackSelectionManagerTransferRequest(managerId)) }
            .isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NO_CHANGE)
    }

    @Test
    fun `매니저가 아니면 양도할 수 없다`() {
        selection()

        assertThatThrownBy { sut.transferManager(selectionId, newManagerId, TrackSelectionManagerTransferRequest(managerId)) }
            .isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SETLIST_MEETING_NOT_MANAGER)
    }

    @Test
    fun `수정 API 로는 매니저가 변경되지 않는다`() {
        val selection = selection()

        sut.updateSelection(selectionId, managerId, TrackSelectionUpdateRequest(title = "새 제목"))

        assertThat(selection.title).isEqualTo("새 제목")
        assertThat(selection.managerId).isEqualTo(managerId)
    }

    private fun selection(): TrackSelection {
        val selection = TrackSelection.create(title = "회의", managerId = managerId)
        val field = selection.javaClass.getDeclaredField("id")
        field.isAccessible = true
        field.set(selection, selectionId)
        `when`(selectionRepository.findById(selectionId)).thenReturn(Optional.of(selection))
        return selection
    }
}
