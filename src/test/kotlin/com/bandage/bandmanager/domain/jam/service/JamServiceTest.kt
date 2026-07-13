package com.bandage.bandmanager.domain.jam.service

import com.bandage.bandmanager.domain.band.repository.BandMemberRepository
import com.bandage.bandmanager.domain.jam.dto.req.JamMemberAddRequest
import com.bandage.bandmanager.domain.jam.dto.req.JamSessionAddRequest
import com.bandage.bandmanager.domain.jam.dto.req.JamSessionUpdateRequest
import com.bandage.bandmanager.domain.jam.dto.req.JamVenueUpdateRequest
import com.bandage.bandmanager.domain.jam.model.Jam
import com.bandage.bandmanager.domain.jam.model.JamParticipant
import com.bandage.bandmanager.domain.jam.repository.JamParticipantRepository
import com.bandage.bandmanager.domain.jam.repository.JamRepository
import com.bandage.bandmanager.domain.member.service.MemberService
import com.bandage.bandmanager.global.common.domain.SessionDef
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
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class JamServiceTest {
    private val jamRepository = mock(JamRepository::class.java)
    private val jamParticipantRepository = mock(JamParticipantRepository::class.java)
    private val bandMemberRepository = mock(BandMemberRepository::class.java)
    private val jamReservationSyncService = mock(JamReservationSyncService::class.java)
    private val memberService = mock(MemberService::class.java)
    private val sut =
        JamService(
            jamRepository,
            jamParticipantRepository,
            bandMemberRepository,
            jamReservationSyncService,
            memberService,
        )

    private val jamId = UUID.randomUUID()

    private fun jam(sessions: List<SessionDef> = emptyList()): Jam {
        val jam =
            Jam.create(
                title = "합주",
                trackInfo = TrackInfo(title = "곡", artist = "아티스트"),
                startAt = LocalDateTime.of(2026, 6, 10, 19, 0),
                durationMinutes = 120,
                venue = null,
                sessions = sessions,
            )
        val field = Jam::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(jam, jamId)
        return jam
    }

    @Test
    fun `합주 참여자가 아니면 합주를 수정할 수 없다`() {
        `when`(jamRepository.findById(jamId)).thenReturn(Optional.of(jam()))
        // existsByJamAndMember 미스텁 → 기본 false (비참여자)

        assertThatThrownBy { sut.updateVenue(jamId, JamVenueUpdateRequest("홍대 스튜디오"), 99L) }
            .isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.JAM_FORBIDDEN_NOT_PARTICIPANT)
    }

    @Test
    fun `합주 참여자면 합주를 수정할 수 있다`() {
        val jam = jam()
        `when`(jamRepository.findById(jamId)).thenReturn(Optional.of(jam))
        `when`(jamParticipantRepository.existsByJamAndMember(jam, 1L)).thenReturn(true)

        sut.updateVenue(jamId, JamVenueUpdateRequest("홍대 스튜디오"), 1L)

        assertThat(jam.timeInfo.venue).isEqualTo("홍대 스튜디오")
    }

    @Test
    fun `세션을 추가할 수 있다`() {
        val jam = jam()
        `when`(jamRepository.findById(jamId)).thenReturn(Optional.of(jam))
        `when`(jamParticipantRepository.existsByJamAndMember(jam, 1L)).thenReturn(true)

        sut.addSession(jamId, JamSessionAddRequest("G-2", "기타", "G", false), 1L)

        assertThat(jam.sessions.map { it.sessionId }).containsExactly("G-2")
    }

    @Test
    fun `이미 존재하는 세션 토큰은 추가할 수 없다`() {
        val jam = jam(listOf(SessionDef("G-1", "기타", "G", false)))
        `when`(jamRepository.findById(jamId)).thenReturn(Optional.of(jam))
        `when`(jamParticipantRepository.existsByJamAndMember(jam, 1L)).thenReturn(true)

        assertThatThrownBy { sut.addSession(jamId, JamSessionAddRequest("G-1", "기타", "G", false), 1L) }
            .isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.JAM_SESSION_ALREADY_EXISTS)
    }

    @Test
    fun `세션 이름과 약칭을 개별로 수정할 수 있다`() {
        val jam = jam(listOf(SessionDef("G-1", "기타", "G", false)))
        `when`(jamRepository.findById(jamId)).thenReturn(Optional.of(jam))
        `when`(jamParticipantRepository.existsByJamAndMember(jam, 1L)).thenReturn(true)

        sut.updateSession(jamId, "G-1", JamSessionUpdateRequest("메인 기타", "MG"), 1L)

        val session = jam.sessions.first { it.sessionId == "G-1" }
        assertThat(session.label).isEqualTo("메인 기타")
        assertThat(session.short).isEqualTo("MG")
    }

    @Test
    fun `존재하지 않는 세션은 수정할 수 없다`() {
        val jam = jam()
        `when`(jamRepository.findById(jamId)).thenReturn(Optional.of(jam))
        `when`(jamParticipantRepository.existsByJamAndMember(jam, 1L)).thenReturn(true)

        assertThatThrownBy { sut.updateSession(jamId, "G-1", JamSessionUpdateRequest("기타", "G"), 1L) }
            .isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.JAM_SESSION_NOT_FOUND)
    }

    @Test
    fun `세션을 삭제하면 배정된 참여자는 세션 미배정 상태로 전환된다`() {
        val jam = jam(listOf(SessionDef("G-1", "기타", "G", false)))
        val participant = JamParticipant.create(jam = jam, sessionId = "G-1", member = 2L)
        `when`(jamRepository.findById(jamId)).thenReturn(Optional.of(jam))
        `when`(jamParticipantRepository.existsByJamAndMember(jam, 1L)).thenReturn(true)
        `when`(jamParticipantRepository.findAllByJam(jam)).thenReturn(listOf(participant))

        sut.removeSession(jamId, "G-1", 1L)

        assertThat(jam.sessions).isEmpty()
        assertThat(participant.sessionId).isNull()
        verify(jamParticipantRepository, never()).delete(participant)
    }

    @Test
    fun `이미 다른 멤버가 배정된 세션에는 추가로 배정할 수 없다`() {
        val jam = jam(listOf(SessionDef("G-1", "기타", "G", false)))
        `when`(jamRepository.findById(jamId)).thenReturn(Optional.of(jam))
        `when`(jamParticipantRepository.existsByJamAndMember(jam, 1L)).thenReturn(true)
        `when`(jamParticipantRepository.existsByJamAndSessionId(jam, "G-1")).thenReturn(true)

        assertThatThrownBy { sut.addParticipant(jamId, JamMemberAddRequest("G-1", 2L), 1L) }
            .isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.JAM_SESSION_FULL)
    }
}
