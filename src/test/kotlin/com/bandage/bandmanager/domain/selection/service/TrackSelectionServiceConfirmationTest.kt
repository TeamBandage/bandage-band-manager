package com.bandage.bandmanager.domain.selection.service

import com.bandage.bandmanager.domain.band.repository.BandMemberRepository
import com.bandage.bandmanager.domain.member.service.MemberService
import com.bandage.bandmanager.domain.selection.dto.req.SetlistConfirmationUpdateRequest
import com.bandage.bandmanager.domain.selection.dto.req.TrackSelectionItemSelectionRequest
import com.bandage.bandmanager.domain.selection.model.PracticeWindow
import com.bandage.bandmanager.domain.selection.model.TrackSelection
import com.bandage.bandmanager.domain.selection.model.TrackSelectionItem
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionBandRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionItemApplicantRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionItemChatMessageRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionItemConfirmationRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionItemRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionMemberRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionRepository
import com.bandage.bandmanager.global.common.domain.SessionDef
import com.bandage.bandmanager.global.common.domain.TrackInfo
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

/** 세션 슬롯화(1세션=1인) 이후 정원 검증(existsByItemAndSessionId 기반) 회귀 테스트. */
class TrackSelectionServiceConfirmationTest {
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
    private val selectionId: UUID = UUID.randomUUID()
    private val itemId: UUID = UUID.randomUUID()

    private fun setEntityId(
        target: Any,
        id: UUID,
    ) {
        val field = target.javaClass.getDeclaredField("id")
        field.isAccessible = true
        field.set(target, id)
    }

    private fun selection(): TrackSelection {
        val selection =
            TrackSelection.create(
                title = "회의",
                managerId = managerId,
                practiceWindow = PracticeWindow(from = LocalDate.of(2026, 1, 1), to = LocalDate.of(2026, 1, 31)),
            )
        setEntityId(selection, selectionId)
        return selection
    }

    private fun item(selection: TrackSelection): TrackSelectionItem {
        val item = mock(TrackSelectionItem::class.java)
        `when`(item.id).thenReturn(itemId)
        `when`(item.selection).thenReturn(selection)
        `when`(item.proposerId).thenReturn(managerId)
        `when`(item.trackInfo).thenReturn(TrackInfo(title = "곡", artist = "아티스트"))
        `when`(item.note).thenReturn(null)
        `when`(item.isSelected).thenReturn(false)
        `when`(item.sessions).thenReturn(listOf(SessionDef("G-1", "기타", "G", false)))
        return item
    }

    @Test
    fun `이미 확정자가 있는 세션에 추가로 확정하려 하면 정원 초과 예외가 발생한다`() {
        val selection = selection()
        val item = item(selection)
        `when`(selectionRepository.findById(selectionId)).thenReturn(Optional.of(selection))
        `when`(itemRepository.findById(itemId)).thenReturn(Optional.of(item))
        `when`(confirmationRepository.existsByItemAndSessionId(item, "G-1")).thenReturn(true)

        assertThatThrownBy {
            sut.updateConfirmations(
                selectionId,
                itemId,
                "G-1",
                managerId,
                SetlistConfirmationUpdateRequest(confirm = listOf(2L)),
            )
        }.isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SETLIST_MEETING_ITEM_SESSION_FULL)
    }

    @Test
    fun `빈 세션에는 확정자를 추가할 수 있다`() {
        val selection = selection()
        val item = item(selection)
        `when`(selectionRepository.findById(selectionId)).thenReturn(Optional.of(selection))
        `when`(itemRepository.findById(itemId)).thenReturn(Optional.of(item))
        `when`(confirmationRepository.existsByItemAndSessionId(item, "G-1")).thenReturn(false)
        `when`(applicantRepository.findAllByItem(item)).thenReturn(emptyList())
        `when`(confirmationRepository.findAllByItem(item)).thenReturn(emptyList())
        `when`(memberService.getMemberSummaries(setOf(managerId))).thenReturn(emptyMap())

        sut.updateConfirmations(
            selectionId,
            itemId,
            "G-1",
            managerId,
            SetlistConfirmationUpdateRequest(confirm = listOf(2L)),
        )
    }

    @Test
    fun `세션 중 하나라도 확정자가 없으면 선택 확정할 수 없다`() {
        val selection = selection()
        val item = item(selection)
        `when`(selectionRepository.findById(selectionId)).thenReturn(Optional.of(selection))
        `when`(itemRepository.findById(itemId)).thenReturn(Optional.of(item))
        `when`(confirmationRepository.existsByItemAndSessionId(item, "G-1")).thenReturn(false)

        assertThatThrownBy {
            sut.updateItemSelection(
                selectionId,
                itemId,
                managerId,
                TrackSelectionItemSelectionRequest(selected = true),
            )
        }.isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SETLIST_SELECTION_INCOMPLETE_SESSION)
    }
}
