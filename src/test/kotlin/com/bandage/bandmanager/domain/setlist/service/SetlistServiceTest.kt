package com.bandage.bandmanager.domain.setlist.service

import com.bandage.bandmanager.domain.band.repository.BandMemberRepository
import com.bandage.bandmanager.domain.member.service.MemberService
import com.bandage.bandmanager.domain.setlist.dto.req.SetlistUpdateRequest
import com.bandage.bandmanager.domain.setlist.model.Setlist
import com.bandage.bandmanager.domain.setlist.model.SetlistBand
import com.bandage.bandmanager.domain.setlist.model.SetlistTrack
import com.bandage.bandmanager.domain.setlist.model.SetlistTrackParticipant
import com.bandage.bandmanager.domain.setlist.repository.SetlistBandRepository
import com.bandage.bandmanager.domain.setlist.repository.SetlistRepository
import com.bandage.bandmanager.domain.setlist.repository.SetlistTrackParticipantRepository
import com.bandage.bandmanager.domain.setlist.repository.SetlistTrackRepository
import com.bandage.bandmanager.global.common.domain.TrackInfo
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.Optional
import java.util.UUID

class SetlistServiceTest {
    private val setlistRepository = mock(SetlistRepository::class.java)
    private val setlistBandRepository = mock(SetlistBandRepository::class.java)
    private val setlistTrackRepository = mock(SetlistTrackRepository::class.java)
    private val setlistTrackParticipantRepository = mock(SetlistTrackParticipantRepository::class.java)
    private val bandMemberRepository = mock(BandMemberRepository::class.java)
    private val memberService = mock(MemberService::class.java)

    private val sut =
        SetlistService(
            setlistRepository,
            setlistBandRepository,
            setlistTrackRepository,
            setlistTrackParticipantRepository,
            bandMemberRepository,
            memberService,
        )

    private val setlistId: UUID = UUID.fromString("00000000-0000-0000-0000-0000000000a1")
    private val managerId = 1L
    private val newManagerId = 2L

    private fun setlist(): Setlist {
        val setlist = Setlist.create(trackSelectionId = UUID.randomUUID(), title = "셋리스트", managerId = managerId)
        val field = setlist.javaClass.getDeclaredField("id")
        field.isAccessible = true
        field.set(setlist, setlistId)
        `when`(setlistRepository.findById(setlistId)).thenReturn(Optional.of(setlist))
        return setlist
    }

    @Test
    fun `updateSetlist - managerId 를 전달하면 접근 가능한 멤버에게 매니저 권한을 양도한다`() {
        val setlist = setlist()
        `when`(setlistRepository.isAccessibleMember(setlistId, newManagerId)).thenReturn(true)

        sut.updateSetlist(setlistId, managerId, SetlistUpdateRequest(title = setlist.title, managerId = newManagerId))

        assertThat(setlist.managerId).isEqualTo(newManagerId)
    }

    @Test
    fun `updateSetlist - 접근 불가한 멤버로는 양도할 수 없다`() {
        val setlist = setlist()
        `when`(setlistRepository.isAccessibleMember(setlistId, newManagerId)).thenReturn(false)

        val ex =
            assertThrows<BusinessException> {
                sut.updateSetlist(setlistId, managerId, SetlistUpdateRequest(title = setlist.title, managerId = newManagerId))
            }
        assertThat(ex.errorCode).isEqualTo(ErrorCode.SETLIST_MANAGER_NOT_PARTICIPANT)
    }

    @Test
    fun `updateSetlist - 본인에게 양도하면 NO_CHANGE`() {
        val setlist = setlist()

        val ex =
            assertThrows<BusinessException> {
                sut.updateSetlist(setlistId, managerId, SetlistUpdateRequest(title = setlist.title, managerId = managerId))
            }
        assertThat(ex.errorCode).isEqualTo(ErrorCode.NO_CHANGE)
    }

    @Test
    fun `updateSetlist - 매니저가 아니면 SETLIST_NOT_MANAGER`() {
        val setlist = setlist()

        val ex =
            assertThrows<BusinessException> {
                sut.updateSetlist(setlistId, newManagerId, SetlistUpdateRequest(title = setlist.title, managerId = null))
            }
        assertThat(ex.errorCode).isEqualTo(ErrorCode.SETLIST_NOT_MANAGER)
    }

    @Test
    fun `deleteSetlist - 매니저는 셋리스트와 트랙·참여자·밴드 연결을 함께 소프트 삭제한다`() {
        val setlist = setlist()
        val track = SetlistTrack.create(setlist, TrackInfo(title = "곡", artist = "아티스트"), note = null, sessions = emptyList())
        val participant = SetlistTrackParticipant.create(track, sessionId = "vocal", memberId = 9L)
        val setlistBand = SetlistBand.create(UUID.randomUUID(), setlistId)
        `when`(setlistTrackRepository.findAllBySetlist(setlist)).thenReturn(listOf(track))
        `when`(setlistTrackParticipantRepository.findAllByTrackIn(listOf(track))).thenReturn(listOf(participant))
        `when`(setlistBandRepository.findAllBySetlistId(setlistId)).thenReturn(listOf(setlistBand))

        sut.deleteSetlist(setlistId, managerId)

        assertThat(setlist.deletedAt).isNotNull()
        assertThat(track.deletedAt).isNotNull()
        assertThat(participant.deletedAt).isNotNull()
        assertThat(setlistBand.deletedAt).isNotNull()
    }

    @Test
    fun `deleteSetlist - 매니저가 아니면 SETLIST_NOT_MANAGER`() {
        val setlist = setlist()

        val ex = assertThrows<BusinessException> { sut.deleteSetlist(setlistId, newManagerId) }

        assertThat(ex.errorCode).isEqualTo(ErrorCode.SETLIST_NOT_MANAGER)
        assertThat(setlist.deletedAt).isNull()
    }
}
